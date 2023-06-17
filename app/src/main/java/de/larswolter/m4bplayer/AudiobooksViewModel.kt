package de.larswolter.m4bplayer

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import at.bitfire.dav4jvm.DavCollection
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.GetLastModified
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.Date

data class Audiobook(
  var localUri: Uri? = null,
  var localSize: Int = 0,
  var localModified: Date? = null,
  var remoteUri: HttpUrl? = null,
  var remoteModified: Date? = null,
  var remoteSize: Int = 0,
  var name: String,
  var status: String = ""
)


class AudiobooksViewModel(
  application: Application
) : AndroidViewModel(application) {

  private val _files = MutableStateFlow(listOf<Audiobook>())
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
    _files.value.forEach { a -> a.remoteUri = null }

    viewModelScope.launch(Dispatchers.IO) {
      status.value = "checking remote"
      try {
        val davCollection = davCollection(null)
        davCollection.propfind(
          depth = 1,
          DisplayName.NAME,
          GetLastModified.NAME
        ) { response, _ ->
          // This callback will be called for every file in the folder.
          // Use `response.properties` to access the successfully retrieved properties.
          val file = response.hrefName()
          if (file.contains(".m4b")) {

            val existing = _files.value.find({ book -> book.name == file })
            if (existing != null) {
              existing.remoteUri = response.href
              _files.value =
                _files.value.map { if (it.name == existing.name) existing else it }
            } else
              _files.value += Audiobook(remoteUri = response.href, name = file)

          }

        }
      } catch (err: Exception) {
        error.value = err.toString()
        err.printStackTrace()
      }
      status.value = ""
      checkLocal()
    }
  }

  fun checkLocal() {
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
          MediaStore.Audio.Media.SIZE
        )

        // Display files in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        val query = resolver.query(
          audioCollection,
          projection,
          "${MediaStore.Video.Media.DISPLAY_NAME} like '%.m4b'",
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
        error.value = err.toString()
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


    val authHandler = BasicDigestAuthHandler(
      domain = null, // Optional, to only authenticate against hosts with this domain.
      username = username,
      password = password
    )
    val okHttpClient = OkHttpClient.Builder()
      .followRedirects(false)
      .authenticator(authHandler)
      .addNetworkInterceptor(authHandler)
      .build()
    return if (book == null || book.remoteUri == null)
      DavCollection(okHttpClient, davUrl.toHttpUrl())
    else
      DavCollection(okHttpClient, book.remoteUri!!)
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
            resolver.openOutputStream(localUri, "w").use { stream ->
              if (stream != null) {
                fileStream.copyTo(stream)
                stream.close()
                file.status = "stored locally"
                file.localUri = localUri
              } else {
                file.status = "no output stream"
              }
              _files.value =
                _files.value.map { if (it.name == file.name) file else it }
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