package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class WatchVideoExtensionPackage(
  val localCachedUriString: String,
  val frameThumbnails: List<Bitmap>,
  val extractedFrameCount: Int,
  val targetResolutionPx: Int,
  val targetFps: Int,
  val estimatedPackageKb: Int,
  val oledDarkPixelRatioPct: Int,
)

object VideoExtensionTranscoder {

  suspend fun cachePickedVideoToPrivateStorage(
    context: Context,
    sourceUri: Uri,
    fileName: String = "active_video.mp4",
  ): File? = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
    val outFile = File(dir, fileName)
    return@withContext runCatching {
      context.contentResolver.openInputStream(sourceUri)?.use { input ->
        FileOutputStream(outFile).use { output ->
          input.copyTo(output, bufferSize = 64 * 1024)
        }
      }
      if (outFile.exists() && outFile.length() > 0L) outFile else null
    }.getOrNull()
  }

  suspend fun buildWatchFrameExtensionPreview(
    context: Context,
    videoUri: Uri,
    targetResolutionPx: Int,
    targetFps: Int,
    sampleCount: Int = 6,
  ): WatchVideoExtensionPackage? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    return@withContext try {
      retriever.setDataSource(context, videoUri)
      val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        ?.toLongOrNull()?.coerceAtLeast(500L) ?: 3000L
      val clipMs = durationMs.coerceAtMost(5000L)

      val frames = mutableListOf<Bitmap>()
      var darkPixels = 0
      var sampledPixels = 0
      var compressedBytes = 0

      for (i in 0 until sampleCount) {
        val timeUs = ((clipMs * i) / sampleCount.coerceAtLeast(1)) * 1000L
        val rawFrame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
          ?: continue
        val scaled = Bitmap.createScaledBitmap(
          rawFrame,
          targetResolutionPx.coerceIn(128, 456),
          targetResolutionPx.coerceIn(128, 456),
          true,
        )
        if (rawFrame !== scaled) rawFrame.recycle()

        val step = 16
        for (y in 0 until scaled.height step step) {
          for (x in 0 until scaled.width step step) {
            val px = scaled.getPixel(x, y)
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val luma = (r * 299 + g * 587 + b * 114) / 1000
            if (luma < 40) darkPixels++
            sampledPixels++
          }
        }

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 72, baos)
        compressedBytes += baos.size()
        frames.add(scaled)
      }

      if (frames.isEmpty()) return@withContext null

      val estimatedTotalFrames = ((clipMs / 1000f) * targetFps).toInt().coerceIn(12, 150)
      val avgFrameKb = (compressedBytes / frames.size) / 1024f
      val estimatedPkgKb = (avgFrameKb * estimatedTotalFrames).toInt().coerceAtLeast(32)
      val darkPct = if (sampledPixels > 0) ((darkPixels * 100f) / sampledPixels).toInt() else 50

      WatchVideoExtensionPackage(
        localCachedUriString = videoUri.toString(),
        frameThumbnails = frames,
        extractedFrameCount = estimatedTotalFrames,
        targetResolutionPx = targetResolutionPx,
        targetFps = targetFps,
        estimatedPackageKb = estimatedPkgKb,
        oledDarkPixelRatioPct = darkPct,
      )
    } catch (_: Exception) {
      null
    } finally {
      runCatching { retriever.release() }
    }
  }
}
