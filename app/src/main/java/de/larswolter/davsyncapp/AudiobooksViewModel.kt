package de.larswolter.davsyncapp

import android.app.Application
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.documentfile.provider.DocumentFile
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
  var error: Boolean = false,
  var syncing: Boolean = false
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
          if (!(remoteBook.remoteType == null && remoteBook.remoteSize.compareTo(0)==0)) {
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
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication())
    val targetUri = sharedPreferences.getString("targetUri", null)
    if (targetUri == null) {
      status.value = "missing target folder"
      return
    }

    _files.value.forEach { a -> a.localUri = null }
    viewModelScope.launch(Dispatchers.IO) {
      status.value = "checking local"
      try {

        val pickedDir = DocumentFile.fromTreeUri(
          getApplication<Application>().applicationContext,
          Uri.parse(targetUri)
        )
        if (pickedDir != null) {
          val files = pickedDir.listFiles()
          for (file in files) {
            if (file.name != null && file.isFile) {
              val name: String = file.name!!
              println("local:" + name)

              val contentUri: Uri = file.uri

              // Stores column values and the contentUri in a local object
              // that represents the media file.
              val existing = _files.value.find({ book -> book.name == name })
              if (existing != null) {
                existing.localUri = contentUri
                _files.value =
                  _files.value.map { if (it.name == existing.name) existing else it }
              } else
                _files.value += Audiobook(localUri = contentUri, name = name)
            }
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

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication())
    val deleteLocal: Boolean = sharedPreferences.getBoolean("deleteLocal", false)
    val targetUri = sharedPreferences.getString("targetUri", null)

    if (deleteLocal && file.remoteUri == null && file.localUri != null) {
      DocumentFile.fromSingleUri(getApplication<Application>().applicationContext, file.localUri!!)
        ?.delete()
      _files.value.drop(idx)
      downloadFile(idx)
      return
    }
    val pickedDir = DocumentFile.fromTreeUri(
      getApplication<Application>().applicationContext,
      Uri.parse(targetUri)
    )
    if (pickedDir != null && file.remoteUri != null && file.localUri == null) {
      file.syncing = true
      _files.value =
        _files.value.map { if (it.name == file.name) file else it }
      status.value = "Syncing file " + file.name
      println("attempting download of " + file.name)
      val davCollection = davCollection(file)
      davCollection.get(accept = "", headers = null) { response ->
        println("got response for " + file.name)
        val fileStream = response.body?.byteStream()
        if (fileStream != null) {

          val localFile = pickedDir.createFile(file.remoteType.toString(), file.name)

// "w" for write.
          if (localFile != null) {
            println("Downloading " + file.name)
            resolver.openOutputStream(localFile.uri, "w").use { stream ->
              if (stream != null) {
                try {
                  fileStream.copyTo(stream)
                  stream.close()
                  file.status = "stored locally"
                  file.localUri = localFile.uri
                } catch (err: Exception) {
                  file.status = "Error downloading " + err.toString()
                  file.error = true
                  resolver.delete(localFile.uri, null)
                }
              } else {
                file.status = "no output stream"
                file.error = true
                resolver.delete(localFile.uri, null)
              }
              file.syncing = false
              _files.value =
                _files.value.map { if (it.name == file.name) file else it }
            }
          }
        }
      }
    } else {
      file.syncing = false
      file.status = "no input stream"
      _files.value =
        _files.value.map { if (it == file) file else it }
    }
    downloadFile(idx + 1)
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