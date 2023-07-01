package de.larswolter.davsyncapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFrame(
  navController: NavController,
  audiobooks: AudiobooksViewModel,
  progress: Boolean = false,
  back: Boolean = false,
  content: @Composable () -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  Scaffold(
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    },

    topBar = {
      TopAppBar(
        title = {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("WebDAV Sync App")
            if (progress)
              CircularProgressIndicator(Modifier.size(24.dp))
          }
        },
        navigationIcon = {
          if (back) {
            IconButton(onClick = {
              navController.navigateUp()
              audiobooks.check()
            }) {
              Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
          }
        },
        actions = {
          if (!back) {
            IconButton(onClick = { expanded = true }) {
              Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(
              expanded = expanded,
              onDismissRequest = { expanded = false }

            ) {
              DropdownMenuItem(text = { Text("Refresh") }, onClick = {
                audiobooks.check()
                expanded = false
              })
              DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                  navController.navigate("settings")
                  expanded = false
                })
              Divider()
              DropdownMenuItem(
                text = { Text("Send Feedback") },
                onClick = { expanded = false })
            }
          }
        },
      )
    },

    floatingActionButton = {
      if (!back) {
        Button(
          onClick = { audiobooks.download(snackbarHostState) },
          modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
          enabled = !progress,
          shape = CircleShape

        ) {
          Icon(painterResource(R.drawable.baseline_download_24), "Sync from Server")

        }
      }
    }, content = { padding ->
      Surface(
        Modifier
          .padding(padding)
      ) { content() }
    })
}