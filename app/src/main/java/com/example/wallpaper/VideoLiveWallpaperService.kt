package com.example.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

/**
 * Standard Android [WallpaperService] extension allowing ChronoCanvas to plug into
 * existing system wallpaper pickers and render looping, muted, OLED-optimized video wallpapers.
 */
class VideoLiveWallpaperService : WallpaperService() {

  override fun onCreateEngine(): Engine = VideoWallpaperEngine()

  inner class VideoWallpaperEngine : Engine() {
    private var mediaPlayer: MediaPlayer? = null
    private var currentVideoPath: String? = null

    override fun onSurfaceCreated(holder: SurfaceHolder) {
      super.onSurfaceCreated(holder)
      setupMediaOrFallback(holder)
    }

    override fun onVisibilityChanged(visible: Boolean) {
      super.onVisibilityChanged(visible)
      val player = mediaPlayer
      if (visible) {
        val activeFile = resolveActiveVideoFile()
        if (activeFile?.absolutePath != currentVideoPath) {
          setupMediaOrFallback(surfaceHolder)
        } else {
          runCatching {
            if (player != null && !player.isPlaying) {
              player.start()
            }
          }
        }
      } else {
        // Pause immediately when hidden to eliminate background GPU/decoder battery drain
        runCatching {
          if (player != null && player.isPlaying) {
            player.pause()
          }
        }
      }
    }

    override fun onSurfaceDestroyed(holder: SurfaceHolder) {
      releasePlayer()
      super.onSurfaceDestroyed(holder)
    }

    override fun onDestroy() {
      releasePlayer()
      super.onDestroy()
    }

    private fun resolveActiveVideoFile(): File? {
      val file = File(applicationContext.filesDir, "wallpapers/active_video.mp4")
      return if (file.exists() && file.length() > 0L) file else null
    }

    private fun setupMediaOrFallback(holder: SurfaceHolder) {
      releasePlayer()
      val file = resolveActiveVideoFile()
      if (file == null) {
        drawFallbackFrame(holder)
        return
      }
      currentVideoPath = file.absolutePath
      runCatching {
        MediaPlayer().apply {
          setDataSource(applicationContext, Uri.fromFile(file))
          setSurface(holder.surface)
          isLooping = true
          setVolume(0f, 0f)
          setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
          prepareAsync()
          setOnPreparedListener { mp ->
            if (isVisible) mp.start()
          }
        }
      }.onSuccess { created ->
        mediaPlayer = created
      }.onFailure {
        drawFallbackFrame(holder)
      }
    }

    private fun drawFallbackFrame(holder: SurfaceHolder) {
      val canvas: Canvas? = runCatching { holder.lockCanvas() }.getOrNull()
      if (canvas != null) {
        try {
          canvas.drawColor(Color.rgb(7, 9, 14))
          val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(110, 62, 240, 208)
            style = Paint.Style.STROKE
            strokeWidth = 6f
          }
          val cx = canvas.width / 2f
          val cy = canvas.height / 2f
          val radius = minOf(cx, cy) * 0.62f
          canvas.drawCircle(cx, cy, radius, paint)
        } finally {
          runCatching { holder.unlockCanvasAndPost(canvas) }
        }
      }
    }

    private fun releasePlayer() {
      runCatching {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        mediaPlayer?.release()
      }
      mediaPlayer = null
    }
  }
}
