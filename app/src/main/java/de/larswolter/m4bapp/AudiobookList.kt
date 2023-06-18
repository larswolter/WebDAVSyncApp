package de.larswolter.m4bapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat

@Composable
fun AudiobookList(list: List<Audiobook>) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
  ) {

    for (x in list) {
      var localColor = Color.Gray
      if (x.localUri != null) localColor = Color.Green
      var remoteColor = Color.Gray
      if (x.remoteUri != null) remoteColor = Color.Green
      item {
        Surface() {
          Column() {
            Divider(thickness = Dp.Hairline)
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.padding(8.dp)
            ) {
              Text(text = x.name, modifier = Modifier.weight(1f))
              Row {
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
            Row(
              modifier = Modifier
                .padding(8.dp)
            ) {
              if (x.remoteType != null) Text(
                text = x.remoteType.toString() + "  ",
                fontSize = 12.sp
              )
              if (x.remoteModified != null) Text(
                text = DateFormat.getDateInstance().format("dd.MM.yyyy HH:mm").format(
                  x.remoteModified!!
                ) + "  ", fontSize = 12.sp
              )
              if (x.remoteSize.compareTo(0) != 0) {
                if (x.remoteSize > 1024 * 1024 * 2) {
                  Text(
                    text = (x.remoteSize / 1024 / 1024).toString() + " MB",
                    fontSize = 12.sp
                  )
                } else Text(
                  text = (x.remoteSize / 1024).toString() + " KB",
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }
    }
  }
}