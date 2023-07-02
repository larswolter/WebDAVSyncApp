package de.larswolter.davsyncapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookList(list: List<Audiobook>) {
  var countLocal = 0
  var countRemote = 0
  for (x in list) {
    if (x.localUri != null) countLocal++
    if (x.remoteUri != null) countRemote++
  }
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
  ) {
    item {
      Text(text = "M4B Audiobooks:$countLocal local and $countRemote remote")
    }
    for (x in list) {
      var localColor = Color.Gray
      if (x.localUri != null) localColor = Color.Black
      var remoteColor = Color.Gray
      if (x.remoteUri != null) remoteColor = Color.Black
      var supportingText = x.status+" - "
      if (x.remoteType != null) supportingText += x.remoteType.toString() + "  "
      if (x.remoteModified != null) supportingText += DateFormat.getDateInstance()
        .format(x.remoteModified!!) + "  "
      if (x.remoteSize.compareTo(0) != 0) {
        if (x.remoteSize > 1024 * 1024 * 2) {
          supportingText += (x.remoteSize / 1024 / 1024).toString() + " MB"
        } else
          supportingText += (x.remoteSize / 1024).toString() + " kB"
      }

      item {
        Divider(thickness = Dp.Hairline)
        ListItem(
          headlineText = { Text(text = x.name) },
          supportingText = { Text(text = supportingText) },
          trailingContent = {
            Row {
              if(x.error) {
                Icon(
                  Icons.Default.Warning,
                  contentDescription = "Local available",
                  tint = Color.Red
                )
              } else if(x.syncing) {
                CircularProgressIndicator(Modifier.size(24.dp))
              } else {
                Icon(
                  painterResource(R.drawable.baseline_sd_storage_24),
                  contentDescription = "Local available",
                  tint = localColor
                )
                Icon(
                  painterResource(R.drawable.baseline_cloud_24),
                  contentDescription = "Local available",
                  tint = remoteColor
                )
              }
            }
          }

        )
      }
    }
  }
}