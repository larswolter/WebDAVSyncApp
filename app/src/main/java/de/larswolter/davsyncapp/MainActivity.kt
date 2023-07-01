package de.larswolter.davsyncapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val navController = rememberNavController()
      val viewModel = AudiobooksViewModel(application)
      val list by viewModel.files.collectAsState()
      val error by viewModel.error.collectAsState()
      val status by viewModel.status.collectAsState()

      MaterialTheme {
        NavHost(navController = navController, startDestination = "list") {
          composable("list") {
            AppFrame(navController, audiobooks = viewModel, progress = status != "") {
              if (error == "missing settings") {
                Column(Modifier.padding(16.dp)) {
                  Text(text = "Missing a WebDav URL in the settings, please configure your remote setup")
                  Button(
                    onClick = { navController.navigate("settings") },
                    modifier = Modifier.fillMaxWidth(),

                    ) {
                    Text(text = "Settings")
                  }
                }
              } else {
                Column {
                  Row(Modifier.padding(16.dp)) {
                    Text(text = "Status:")

                    if (error != "") Text(text = error, color = Color.Red)
                    if (status != "") {
                      Text(text = status, color = Color.Gray, modifier = Modifier.weight(0.7f))
                    }
                  }
                  AudiobookList(
                    list = list,
                  )
                }
              }
            }
          }
          composable("settings") {
            AppFrame(navController, audiobooks = viewModel, back = true) {
              Settings(application)
            }
          }
        }
      }
    }
  }
}

