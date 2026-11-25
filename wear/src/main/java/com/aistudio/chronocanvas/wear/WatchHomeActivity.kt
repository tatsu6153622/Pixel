package com.aistudio.chronocanvas.wear

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aistudio.chronocanvas.shared.model.WallpaperConfig
import com.aistudio.chronocanvas.shared.model.WatchTelemetrySnapshot
import com.aistudio.chronocanvas.shared.ui.SharedWatchHomeCanvas
import kotlinx.coroutines.launch
import java.io.File

class WatchHomeActivity : ComponentActivity(), SensorEventListener {

  private var isInteractiveState by mutableStateOf(true)
  private var isOffWristState by mutableStateOf(false)
  private var sensorManager: SensorManager? = null
  private var offBodySensor: Sensor? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WatchLocalStore.initialize(this)

    sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    offBodySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)

    setContent {
      MaterialTheme(colorScheme = darkColorScheme(background = Color.Black)) {
        val config by WatchLocalStore.configState.collectAsStateWithLifecycle()
        val localVideoPath by WatchLocalStore.localVideoPath.collectAsStateWithLifecycle()
        val offWristVideoPath by WatchLocalStore.offWristVideoPath.collectAsStateWithLifecycle()
        val unreadCount by WatchLocalStore.unreadNotificationCount.collectAsStateWithLifecycle()
        val lastPulseMs by WatchLocalStore.lastNotificationPulseMs.collectAsStateWithLifecycle()
        var showAppDock by remember { mutableStateOf(false) }

        val batteryPct = remember(isInteractiveState) {
          WatchLocalStore.readWatchBatteryPct(this)
        }
        val telemetry = remember(batteryPct, unreadCount, lastPulseMs, isOffWristState) {
          WatchTelemetrySnapshot(
            watchBatteryPct = batteryPct,
            unreadNotificationCount = unreadCount,
            lastNotificationPulseMs = lastPulseMs,
            isOffWrist = isOffWristState,
          )
        }

        val effectiveConfig = remember(config, localVideoPath, offWristVideoPath) {
          val mainWp = if (!localVideoPath.isNullOrBlank()) {
            config.wallpaper.copy(videoUriString = Uri.fromFile(File(localVideoPath!!)).toString())
          } else config.wallpaper
          val autoWp = if (!offWristVideoPath.isNullOrBlank()) {
            config.wallpaperAutomation.copy(
              offWristVideoUriString = Uri.fromFile(File(offWristVideoPath!!)).toString(),
            )
          } else config.wallpaperAutomation
          config.copy(wallpaper = mainWp, wallpaperAutomation = autoWp)
        }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        ) {
          SharedWatchHomeCanvas(
            config = effectiveConfig,
            telemetry = telemetry,
            isAmbientMode = !isInteractiveState,
            showAppDock = showAppDock,
            onToggleAppDock = { showAppDock = it },
            showOuterBezel = false,
            onAppShortcutClick = { shortcut ->
              val launchIntent = packageManager.getLaunchIntentForPackage(shortcut.packageHint)
              if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
              }
              showAppDock = false
            },
            videoWallpaperSlot = { wallpaper, ambient ->
              WearVideoWallpaperSurface(wallpaper = wallpaper, isAmbient = ambient)
            },
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    isInteractiveState = true
    offBodySensor?.let { sensor ->
      sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    lifecycleScope.launch {
      WatchStatusReporter.publishStatusToPhone(this@WatchHomeActivity)
    }
  }

  override fun onPause() {
    isInteractiveState = false
    sensorManager?.unregisterListener(this)
    super.onPause()
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type == Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT) {
      // Value 0.0 means off-body (taken off wrist), 1.0 means on-body
      val value = event.values.firstOrNull() ?: 1.0f
      isOffWristState = value < 0.5f
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
private fun WearVideoWallpaperSurface(
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
      exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(wallpaper.videoUriString)))
      exoPlayer.repeatMode = if (wallpaper.loopEnabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
      exoPlayer.setPlaybackSpeed(wallpaper.playbackSpeed.coerceIn(0.25f, 2f))
      exoPlayer.prepare()
      exoPlayer.playWhenReady = !isAmbient
    }
  }

  LaunchedEffect(isAmbient) {
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
