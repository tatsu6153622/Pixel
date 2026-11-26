package com.aistudio.chronocanvas.wear

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.wear.ambient.AmbientLifecycleObserver
import com.aistudio.chronocanvas.shared.model.AppDockShortcut
import com.aistudio.chronocanvas.shared.model.WallpaperConfig
import com.aistudio.chronocanvas.shared.model.WatchTelemetrySnapshot
import com.aistudio.chronocanvas.shared.ui.SharedWatchHomeCanvas
import kotlinx.coroutines.launch
import java.io.File

class WatchHomeActivity : ComponentActivity(), SensorEventListener {

  private var isAmbientState by mutableStateOf(false)
  private var isForegroundActive by mutableStateOf(true)
  private var ambientTickCounter by mutableIntStateOf(0)
  private var isOffWristState by mutableStateOf(false)
  private var watchBatteryPctState by mutableIntStateOf(85)
  private var sensorManager: SensorManager? = null
  private var offBodySensor: Sensor? = null

  private val watchBatteryReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
      val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
      val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
      if (level >= 0 && scale > 0) {
        watchBatteryPctState = ((level * 100f) / scale).toInt().coerceIn(0, 100)
      }
    }
  }

  private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
    override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
      if (WatchLocalStore.configState.value.launcherSettings.ambientAlwaysOnEnabled) {
        isAmbientState = true
      }
      watchBatteryPctState = WatchLocalStore.readWatchBatteryPct(this@WatchHomeActivity)
    }

    override fun onExitAmbient() {
      isAmbientState = false
      watchBatteryPctState = WatchLocalStore.readWatchBatteryPct(this@WatchHomeActivity)
      lifecycleScope.launch {
        WatchLocalStore.pullCachedTelemetryFromDataLayer(this@WatchHomeActivity)
        WatchStatusReporter.publishStatusToPhone(this@WatchHomeActivity)
      }
    }

    override fun onUpdateAmbient() {
      ambientTickCounter++
      watchBatteryPctState = WatchLocalStore.readWatchBatteryPct(this@WatchHomeActivity)
    }
  }

  private lateinit var ambientObserver: AmbientLifecycleObserver

  private val notificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      val keepAlive = WatchLocalStore.configState.value.launcherSettings.keepAliveHomeMode
      WatchLauncherService.syncServiceState(this, keepAlive)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setShowWhenLocked(true)
    setTurnScreenOn(true)

    WatchLocalStore.initialize(this)
    watchBatteryPctState = WatchLocalStore.readWatchBatteryPct(this)
    ambientObserver = AmbientLifecycleObserver(this, ambientCallback)
    lifecycle.addObserver(ambientObserver)

    sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    offBodySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)

    requestKeepAlivePermissionsIfNeeded()

    setContent {
      MaterialTheme(colorScheme = darkColorScheme(background = Color.Black)) {
        val config by WatchLocalStore.configState.collectAsStateWithLifecycle()
        val localVideoPath by WatchLocalStore.localVideoPath.collectAsStateWithLifecycle()
        val offWristVideoPath by WatchLocalStore.offWristVideoPath.collectAsStateWithLifecycle()
        val unreadCount by WatchLocalStore.unreadNotificationCount.collectAsStateWithLifecycle()
        val lastPulseMs by WatchLocalStore.lastNotificationPulseMs.collectAsStateWithLifecycle()
        val phoneBatteryPct by WatchLocalStore.phoneBatteryPct.collectAsStateWithLifecycle()
        var showAppDock by remember { mutableStateOf(false) }

        val installedWatchShortcuts = remember(config.appDockShortcuts) {
          buildMergedWatchShortcuts(config.appDockShortcuts)
        }

        LaunchedEffect(config.launcherSettings.keepAliveHomeMode) {
          WatchLauncherService.syncServiceState(
            this@WatchHomeActivity,
            config.launcherSettings.keepAliveHomeMode,
          )
        }

        LaunchedEffect(isForegroundActive, isAmbientState) {
          if (isForegroundActive && !isAmbientState) {
            while (true) {
              kotlinx.coroutines.delay(60_000L)
              WatchStatusReporter.requestTelemetryFromPhone(this@WatchHomeActivity)
            }
          }
        }

        val batteryPct = watchBatteryPctState
        val telemetry = remember(batteryPct, phoneBatteryPct, unreadCount, lastPulseMs, isOffWristState) {
          WatchTelemetrySnapshot(
            watchBatteryPct = batteryPct,
            phoneBatteryPct = phoneBatteryPct,
            unreadNotificationCount = unreadCount,
            lastNotificationPulseMs = lastPulseMs,
            isOffWrist = isOffWristState,
          )
        }

        val effectiveConfig = remember(config, localVideoPath, offWristVideoPath, installedWatchShortcuts) {
          val mainWp = if (!localVideoPath.isNullOrBlank()) {
            config.wallpaper.copy(videoUriString = Uri.fromFile(File(localVideoPath!!)).toString())
          } else config.wallpaper
          val autoWp = if (!offWristVideoPath.isNullOrBlank()) {
            config.wallpaperAutomation.copy(
              offWristVideoUriString = Uri.fromFile(File(offWristVideoPath!!)).toString(),
            )
          } else config.wallpaperAutomation
          config.copy(
            wallpaper = mainWp,
            wallpaperAutomation = autoWp,
            appDockShortcuts = installedWatchShortcuts,
          )
        }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        ) {
          SharedWatchHomeCanvas(
            config = effectiveConfig,
            telemetry = telemetry,
            isAmbientMode = isAmbientState,
            showAppDock = showAppDock,
            onToggleAppDock = { showAppDock = it },
            showOuterBezel = false,
            onAppShortcutClick = { shortcut ->
              launchWatchShortcut(shortcut)
              showAppDock = false
            },
            videoWallpaperSlot = { wallpaper, ambient ->
              WearVideoWallpaperSurface(
                wallpaper = wallpaper,
                isAmbient = ambient || !isForegroundActive,
              )
            },
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    isForegroundActive = true
    isAmbientState = ambientObserver.isAmbient
    watchBatteryPctState = WatchLocalStore.readWatchBatteryPct(this)
    runCatching {
      ContextCompat.registerReceiver(
        this,
        watchBatteryReceiver,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_NOT_EXPORTED,
      )
    }
    offBodySensor?.let { sensor ->
      sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    val keepAlive = WatchLocalStore.configState.value.launcherSettings.keepAliveHomeMode
    WatchLauncherService.syncServiceState(this, keepAlive)
    lifecycleScope.launch {
      WatchLocalStore.pullCachedTelemetryFromDataLayer(this@WatchHomeActivity)
      WatchStatusReporter.publishStatusToPhone(this@WatchHomeActivity)
    }
  }

  override fun onPause() {
    isForegroundActive = false
    runCatching { unregisterReceiver(watchBatteryReceiver) }
    if (!ambientObserver.isAmbient) {
      sensorManager?.unregisterListener(this)
    }
    super.onPause()
  }

  override fun onStop() {
    sensorManager?.unregisterListener(this)
    super.onStop()
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type == Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT) {
      val value = event.values.firstOrNull() ?: 1.0f
      isOffWristState = value < 0.5f
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

  private fun requestKeepAlivePermissionsIfNeeded() {
    val keepAlive = WatchLocalStore.configState.value.launcherSettings.keepAliveHomeMode
    if (!keepAlive) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  private fun buildMergedWatchShortcuts(configured: List<AppDockShortcut>): List<AppDockShortcut> {
    val merged = linkedMapOf<String, AppDockShortcut>()
    configured.filter { it.enabled }.forEach { shortcut ->
      if (packageManager.getLaunchIntentForPackage(shortcut.packageHint) != null) {
        merged[shortcut.packageHint] = shortcut
      }
    }
    val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolvedApps = runCatching {
      packageManager.queryIntentActivities(mainIntent, 0)
    }.getOrDefault(emptyList())

    for (info in resolvedApps) {
      val pkg = info.activityInfo?.packageName ?: continue
      if (pkg == packageName || merged.containsKey(pkg)) continue
      val label = info.loadLabel(packageManager)?.toString()?.take(12).orEmpty().ifBlank { pkg }
      val iconKey = when {
        pkg.contains("fitness") || pkg.contains("fitbit") -> "fitness"
        pkg.contains("media") || pkg.contains("music") || pkg.contains("spotify") -> "music"
        pkg.contains("wallet") || pkg.contains("pay") -> "wallet"
        pkg.contains("map") -> "map"
        pkg.contains("clock") || pkg.contains("timer") -> "timer"
        pkg.contains("setting") -> "settings"
        else -> "spark"
      }
      merged[pkg] = AppDockShortcut(
        id = pkg,
        label = label,
        iconKey = iconKey,
        packageHint = pkg,
        enabled = true,
      )
      if (merged.size >= 12) break
    }
    return if (merged.isEmpty()) configured else merged.values.toList()
  }

  private fun launchWatchShortcut(shortcut: AppDockShortcut) {
    val launchIntent = packageManager.getLaunchIntentForPackage(shortcut.packageHint)
    if (launchIntent != null) {
      launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      runCatching { startActivity(launchIntent) }
    }
  }
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
      exoPlayer.playWhenReady = true
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
