package de.larswolter.davsyncapp

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(application: Application) {
  val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
  val prefEdit = sharedPreferences.edit()
  var davUrl by rememberSaveable {
    mutableStateOf(
      sharedPreferences.getString("davUrl", "").toString()
    )
  }
  var username by rememberSaveable {
    mutableStateOf(
      sharedPreferences.getString("username", "").toString()
    )
  }
  var password by rememberSaveable {
    mutableStateOf(
      sharedPreferences.getString("password", "").toString()
    )
  }
  var deleteLocal by rememberSaveable {
    mutableStateOf(
      sharedPreferences.getBoolean("deleteLocal", false)
    )
  }
  Surface(Modifier.fillMaxSize()) {
    Column(
      Modifier
        .fillMaxSize()
        .padding(16.dp)) {
      Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(text="Enter valid webdav url and credentials. The URL must contain the target Folder.")
      }
      OutlinedTextField(
        modifier=Modifier.fillMaxWidth(),
        value = davUrl,
        onValueChange = {
          davUrl = it
          prefEdit.putString("davUrl", it)
          prefEdit.apply()
        },
        label = { Text("WebDAV URL") }
      )
      OutlinedTextField(
        modifier=Modifier.fillMaxWidth(),
        value = username,
        onValueChange = {
          username = it
          prefEdit.putString("username", it)
          prefEdit.apply()
        },
        label = { Text("Username") }
      )
      OutlinedTextField(
        modifier=Modifier.fillMaxWidth(),
        value = password,
        onValueChange = {
          password = it
          prefEdit.putString("password", it)
          prefEdit.apply()
        },
        label = { Text("Password") }
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = deleteLocal, onCheckedChange = {
          deleteLocal = it
          prefEdit.putBoolean("deleteLocal", it)
          prefEdit.apply()

        })
        Text(text = "delete local files if they dont exist remotely")
      }
    }
  }
}