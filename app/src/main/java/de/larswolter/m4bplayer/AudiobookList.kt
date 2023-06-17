package de.larswolter.m4bplayer

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import okhttp3.HttpUrl.Companion.toHttpUrl

@Preview
@Composable
fun AudiobookListPreview() {
  val list = listOf(
    Audiobook(
      name = "Ninjago-1.m4b",
      localUri = "local1".toUri(),
      remoteUri = "https://example.com".toHttpUrl()
    ),
    Audiobook(
      name = "Ninjago-2.m4b",
      remoteUri = "https://example.com".toHttpUrl()
    ),
    Audiobook(
      name = "Ninjago-Special-Abenteuer von Meister Wu.m4b",
      localUri = "local1".toUri(),
      remoteUri = "https://example.com".toHttpUrl()
    ),
    Audiobook(
      name = "Ninjago-3.m4b",
      localUri = "local1".toUri(),
    )
  )
  AudiobookList(list = list)
}

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
          }
        }
      }
    }
  }
}