package de.larswolter.davsyncapp

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import at.bitfire.dav4jvm.DavCollection
import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.GetContentLength
import at.bitfire.dav4jvm.property.GetContentType
import at.bitfire.dav4jvm.property.GetLastModified
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import java.util.Date


data class Audiobook(
  var localUri: Uri? = null,
  var localSize: Int = 0,
  var localModified: Date? = null,
  var remoteUri: HttpUrl? = null,
  var remoteModified: Date? = null,
  var remoteSize: Long = 0,
  var remoteType: MediaType? = null,
  var name: String,
  var status: String = "",
  var error: Boolean = false
) {
}


class AudiobooksViewModel(
  application: Application
) : AndroidViewModel(application) {

  private val _files = MutableStateFlow(listOf<Audiobook>())
  private var okHttpClient: OkHttpClient? = null
  val files: StateFlow<List<Audiobook>> = _files.asStateFlow()
  val error = MutableStateFlow("")
  val status = MutableStateFlow("")

  init {
    check()
  }

  fun check() {
    _files.value = listOf<Audiobook>()
    error.value = ""
    checkRemote()
  }

  fun checkRemote() {
    println("checking remote")
    _files.value.forEach { a -> a.remoteUri = null }

    viewModelScope.launch(Dispatchers.IO) {
      status.value = "checking remote"
      try {
        val davCollection = davCollection(null)
        davCollection.propfind(
          depth = 1,
          DisplayName.NAME,
          GetLastModified.NAME,
          GetContentLength.NAME,
          GetContentType.NAME
        ) { response, _ ->
          // This callback will be called for every file in the folder.
          // Use `response.properties` to access the successfully retrieved properties.
          val file = response.hrefName()
          if (file.contains(".m4b")) {
            val remoteBook = Audiobook(remoteUri = response.href, name = file)
            for (prop: Property in response.properties) {
              if (prop is GetContentType) {
                remoteBook.remoteType = prop.type
              } else if (prop is GetContentLength) {
                remoteBook.remoteSize = prop.contentLength
              } else if (prop is GetLastModified) {
                remoteBook.remoteModified = Date(prop.lastModified)
              }
            }
            println("Remote:" + remoteBook.remoteModified.toString() + ", " + remoteBook.remoteSize.toString() + ", " + remoteBook.remoteType.toString())
            val existing = _files.value.find({ book -> book.name == file })
            if (existing != null) {
              existing.remoteUri = remoteBook.remoteUri
              existing.remoteType = remoteBook.remoteType
              existing.remoteSize = remoteBook.remoteSize
              existing.remoteModified = remoteBook.remoteModified
              _files.value =
                _files.value.map { if (it.name == existing.name) existing else it }
            } else
              _files.value += remoteBook

          }

        }
      } catch (err: Exception) {
        if (err.message != null)
          error.value = err.message!!
        else error.value = err.toString()
        err.printStackTrace()
      }
      status.value = ""
      checkLocal()
    }
  }

  fun checkLocal() {
    println("checking local")
    _files.value.forEach { a -> a.localUri = null }
    viewModelScope.launch(Dispatchers.IO) {
      status.value = "checking local"
      try {
        val resolver = getApplication<Application>().contentResolver

        val audioCollection =
          MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
          )
        val projection = arrayOf(
          MediaStore.Audio.Media._ID,
          MediaStore.Audio.Media.DISPLAY_NAME,
          MediaStore.Audio.Media.DATE_MODIFIED,
          MediaStore.Audio.Media.SIZE,
          MediaStore.Audio.Media.RELATIVE_PATH
        )

        // Display files in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        val query = resolver.query(
          audioCollection,
          projection,
          MediaStore.Audio.Media.DISPLAY_NAME+" like '%.m4b' ",
          null,
          sortOrder
        )
        query?.use { cursor ->
          // Cache column indices.
          val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
          val nameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
          val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

          while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getInt(sizeColumn)
            println("local:"+name+", "+size)

            val contentUri: Uri = ContentUris.withAppendedId(
              MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
              id
            )

            // Stores column values and the contentUri in a local object
            // that represents the media file.
            val existing = _files.value.find({ book -> book.name == name })
            if (existing != null) {
              existing.localSize = size
              existing.localUri = contentUri
              _files.value =
                _files.value.map { if (it.name == existing.name) existing else it }
            } else
              _files.value += Audiobook(localUri = contentUri, name = name, localSize = size)
          }
        }
      } catch (err: Exception) {
        if (err.message != null)
          error.value = err.message!!
        else error.value = err.toString()
        err.printStackTrace()
      }
      status.value = ""
    }
  }

  private fun davCollection(book: Audiobook?): DavCollection {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication())
    val davUrl: String = sharedPreferences.getString("davUrl", "").toString()
    val username: String = sharedPreferences.getString("username", "").toString()
    val password: String = sharedPreferences.getString("password", "").toString()
    if (davUrl == "") throw Exception("missing settings")

    val authHandler = BasicDigestAuthHandler(
      domain = null, // Optional, to only authenticate against hosts with this domain.
      username = username,
      password = password
    )
    if (okHttpClient == null) {
      okHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .authenticator(authHandler)
        .addNetworkInterceptor(authHandler)
        .build()
    }
    return if (book == null || book.remoteUri == null)
      DavCollection(okHttpClient!!, davUrl.toHttpUrl())
    else
      DavCollection(okHttpClient!!, book.remoteUri!!)
  }

  fun downloadFile(idx: Int) {
    if (_files.value.size <= idx) {
      status.value = ""
      return
    }
    val file = _files.value.get(idx)
    val resolver = getApplication<Application>().contentResolver
    // Find all audio files on the primary external storage device.
    val audioCollection =
      MediaStore.Audio.Media.getContentUri(
        MediaStore.VOLUME_EXTERNAL_PRIMARY
      )

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication())
    val deleteLocal: Boolean = sharedPreferences.getBoolean("deleteLocal", false)
    if (deleteLocal && file.remoteUri == null && file.localUri != null) {
      resolver.delete(file.localUri!!, null)
      _files.value.drop(idx)
      downloadFile(idx)
      return
    }
    if (file.remoteUri != null && file.localUri == null) {


      status.value = "Syncing file " + file.name
      println("attempting download of " + file.name)
      val davCollection = davCollection(file)
      davCollection.get(accept = "", headers = null) { response ->
        println("got response for " + file.name)
        val fileStream = response.body?.byteStream()
        if (fileStream != null) {


// Publish a new song.
          val audiobookProps = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Audiobooks")
            put(MediaStore.Audio.Media.IS_AUDIOBOOK, true)
          }

// Keep a handle to the new song's URI in case you need to modify it
// later.
          val localUri = resolver
            .insert(audioCollection, audiobookProps)
// "w" for write.
          if (localUri != null) {
            val cursor = resolver.query(localUri, null, null, null, null)
            var localFilename = ""
            try {
              if (cursor != null && cursor.moveToFirst()) {
                localFilename =
                  cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
              }
            } finally {
              cursor!!.close()
            }
            if (localFilename != file.name) {
              resolver.delete(localUri, null)
              file.status = "file already exists, but cannot be accessed"
              file.error = true
              _files.value =
                _files.value.map { if (it.name == file.name) file else it }
              println(file.status + ": '" + file.name + "' <-> '" + localFilename + "'")

            } else {
              println("Downloading " + file.name)
              resolver.openOutputStream(localUri, "w").use { stream ->
                if (stream != null) {
                  try {
                    fileStream.copyTo(stream)
                    stream.close()
                    file.status = "stored locally"
                    file.localUri = localUri
                  }catch (err:Exception) {
                    file.status = "Error downloading "+err.toString()
                    file.error = true
                    resolver.delete(localUri, null)
                  }
                } else {
                  file.status = "no output stream"
                  file.error = true
                  resolver.delete(localUri, null)
                }
                _files.value =
                  _files.value.map { if (it.name == file.name) file else it }
              }
            }
          }
        } else {
          file.status = "no input stream"
          _files.value =
            _files.value.map { if (it == file) file else it }
        }
        downloadFile(idx + 1)
      }
    } else downloadFile(idx + 1)

  }

  fun download(snackbarHostState: SnackbarHostState) {
    println("attempting download...")
    viewModelScope.launch(Dispatchers.IO) {
      if (_files.value.find { f -> f.localUri == null && f.remoteUri != null } == null) {
        snackbarHostState.showSnackbar(message = "Keine Dateien zum synchronisieren")
      }
      downloadFile(0)
    }
  }
}