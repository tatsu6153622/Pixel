package com.aistudio.chronocanvas.wear

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.aistudio.chronocanvas.shared.codec.WatchConfigCodec
import com.aistudio.chronocanvas.shared.model.WatchHomeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object WatchLocalStore {
  private const val PREFS_NAME = "chronocanvas_watch_prefs"
  private const val KEY_CONFIG_JSON = "active_config_json"
  private const val KEY_VIDEO_PATH = "active_video_path"
  private const val KEY_OFFWRIST_VIDEO_PATH = "offwrist_video_path"

  private val _configState = MutableStateFlow(WatchHomeConfig())
  val configState: StateFlow<WatchHomeConfig> = _configState.asStateFlow()

  private val _localVideoPath = MutableStateFlow<String?>(null)
  val localVideoPath: StateFlow<String?> = _localVideoPath.asStateFlow()

  private val _offWristVideoPath = MutableStateFlow<String?>(null)
  val offWristVideoPath: StateFlow<String?> = _offWristVideoPath.asStateFlow()

  private val _unreadNotificationCount = MutableStateFlow(0)
  val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

  private val _lastNotificationPulseMs = MutableStateFlow(0L)
  val lastNotificationPulseMs: StateFlow<Long> = _lastNotificationPulseMs.asStateFlow()

  fun initialize(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.getString(KEY_CONFIG_JSON, null)?.let { raw ->
      runCatching { WatchConfigCodec.fromJson(raw) }.getOrNull()?.let { parsed ->
        _configState.value = parsed
      }
    }
    prefs.getString(KEY_VIDEO_PATH, null)?.let { path ->
      if (File(path).exists()) _localVideoPath.value = path
    }
    prefs.getString(KEY_OFFWRIST_VIDEO_PATH, null)?.let { path ->
      if (File(path).exists()) _offWristVideoPath.value = path
    }
  }

  fun saveConfigJson(context: Context, json: String) {
    val parsed = runCatching { WatchConfigCodec.fromJson(json) }.getOrNull() ?: return
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_CONFIG_JSON, json)
      .apply()
    _configState.value = parsed
  }

  fun saveVideoFile(context: Context, key: String, file: File) {
    val prefKey = if (key == "offwrist") KEY_OFFWRIST_VIDEO_PATH else KEY_VIDEO_PATH
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(prefKey, file.absolutePath)
      .apply()
    if (key == "offwrist") {
      _offWristVideoPath.value = file.absolutePath
    } else {
      _localVideoPath.value = file.absolutePath
    }
  }

  fun updateNotificationTelemetry(unreadCount: Int, pulseMs: Long) {
    _unreadNotificationCount.value = unreadCount.coerceAtLeast(0)
    if (pulseMs > 0L) {
      _lastNotificationPulseMs.value = pulseMs
    }
  }

  fun readWatchBatteryPct(context: Context): Int {
    val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 85
  }
}
