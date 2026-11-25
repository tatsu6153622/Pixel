package com.example.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aistudio.chronocanvas.shared.model.WallpaperConfig
import com.example.R

data class VideoFileDetails(
  val title: String,
  val durationMs: Long,
  val width: Int,
  val height: Int,
  val sizeBytes: Long,
) {
  val formattedSummary: String
    get() {
      val sec = (durationMs / 1000L).coerceAtLeast(0)
      val mb = sizeBytes / (1024.0 * 1024.0)
      val res = if (width > 0 && height > 0) "${width}×${height}" else "解析済み"
      return "$res · ${sec}秒 · ${"%.1f".format(mb)}MB"
    }
}

fun inspectVideoUri(context: Context, uri: Uri): VideoFileDetails {
  var name = "selected_wallpaper.mp4"
  var sizeBytes = 0L
  runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
      if (cursor.moveToFirst()) {
        if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
        if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
      }
    }
  }

  var durationMs = 0L
  var width = 0
  var height = 0
  val retriever = MediaMetadataRetriever()
  try {
    retriever.setDataSource(context, uri)
    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
    height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
  } catch (_: Exception) {
  } finally {
    runCatching { retriever.release() }
  }

  return VideoFileDetails(
    title = name,
    durationMs = durationMs,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
  )
}

@Composable
fun PhoneVideoWallpaperSurface(
  wallpaper: WallpaperConfig,
  isAmbient: Boolean,
) {
  val context = LocalContext.current
  val exoPlayer = remember(context) {
    ExoPlayer.Builder(context).build().apply {
      volume = 0f
      repeatMode = Player.REPEAT_MODE_ALL
    }
  }

  LaunchedEffect(wallpaper.videoUriString, wallpaper.loopEnabled, wallpaper.playbackSpeed) {
    if (wallpaper.videoUriString.isNotBlank()) {
      runCatching {
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(wallpaper.videoUriString)))
        exoPlayer.repeatMode = if (wallpaper.loopEnabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        exoPlayer.setPlaybackSpeed(wallpaper.playbackSpeed.coerceIn(0.25f, 2f))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = !isAmbient
      }
    }
  }

  LaunchedEffect(isAmbient, wallpaper.pauseInAmbient) {
    if (isAmbient && wallpaper.pauseInAmbient) {
      exoPlayer.pause()
    } else if (!isAmbient && wallpaper.videoUriString.isNotBlank()) {
      exoPlayer.play()
    }
  }

  DisposableEffect(exoPlayer) {
    onDispose { exoPlayer.release() }
  }

  AndroidView(
    factory = { ctx ->
      PlayerView(ctx).apply {
        useController = false
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        layoutParams = FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
        player = exoPlayer
      }
    },
    modifier = Modifier.fillMaxSize(),
  )
}

@Composable
fun PhonePresetArtworkSlot(
  presetArtId: String,
  isAmbient: Boolean,
) {
  val resId = when (presetArtId) {
    "preset_horology" -> R.drawable.img_preset_horology_1795588542472
    else -> R.drawable.img_preset_aurora_1795588541917
  }
  Image(
    painter = painterResource(id = resId),
    contentDescription = "プリセット壁紙アート",
    contentScale = ContentScale.Crop,
    alpha = if (isAmbient) 0.3f else 1f,
    modifier = Modifier.fillMaxSize(),
  )
}
