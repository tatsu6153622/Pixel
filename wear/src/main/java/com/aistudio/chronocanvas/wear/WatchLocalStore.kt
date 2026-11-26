package com.aistudio.chronocanvas.wear

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import com.aistudio.chronocanvas.shared.codec.WatchConfigCodec
import com.aistudio.chronocanvas.shared.model.WatchHomeConfig
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object WatchLocalStore {
  private const val PREFS_NAME = "chronocanvas_watch_prefs"
  private const val KEY_CONFIG_JSON = "active_config_json"
  private const val KEY_VIDEO_PATH = "active_video_path"
  private const val KEY_OFFWRIST_VIDEO_PATH = "offwrist_video_path"
  private const val KEY_UNREAD_NOTIFICATIONS = "unread_notifications"
  private const val KEY_NOTIFICATION_PULSE_MS = "notification_pulse_ms"
  private const val KEY_PHONE_BATTERY_PCT = "phone_battery_pct"

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

  private val _phoneBatteryPct = MutableStateFlow(-1)
  val phoneBatteryPct: StateFlow<Int> = _phoneBatteryPct.asStateFlow()

  fun initialize(context: Context) {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
    _unreadNotificationCount.value = prefs.getInt(KEY_UNREAD_NOTIFICATIONS, 0).coerceAtLeast(0)
    _lastNotificationPulseMs.value = prefs.getLong(KEY_NOTIFICATION_PULSE_MS, 0L)
    _phoneBatteryPct.value = prefs.getInt(KEY_PHONE_BATTERY_PCT, -1)
  }

  fun saveConfigJson(context: Context, json: String) {
    val parsed = runCatching { WatchConfigCodec.fromJson(json) }.getOrNull() ?: return
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_CONFIG_JSON, json)
      .apply()
    _configState.value = parsed
  }

  fun saveVideoFile(context: Context, key: String, file: File) {
    val prefKey = if (key == "offwrist") KEY_OFFWRIST_VIDEO_PATH else KEY_VIDEO_PATH
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(prefKey, file.absolutePath)
      .apply()
    if (key == "offwrist") {
      _offWristVideoPath.value = file.absolutePath
    } else {
      _localVideoPath.value = file.absolutePath
    }
  }

  fun updateNotificationTelemetry(
    unreadCount: Int,
    pulseMs: Long,
    context: Context? = null,
    phoneBatteryPct: Int? = null,
  ) {
    val safeUnread = unreadCount.coerceAtLeast(0)
    _unreadNotificationCount.value = safeUnread

    val resolvedPulse = if (safeUnread == 0 && pulseMs == 0L) {
      0L.also { _lastNotificationPulseMs.value = 0L }
    } else if (pulseMs > 0L) {
      pulseMs.also { _lastNotificationPulseMs.value = it }
    } else {
      _lastNotificationPulseMs.value
    }

    if (phoneBatteryPct != null && phoneBatteryPct in 0..100) {
      _phoneBatteryPct.value = phoneBatteryPct
    }

    if (context != null) {
      val editor = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
      editor.putInt(KEY_UNREAD_NOTIFICATIONS, safeUnread)
      editor.putLong(KEY_NOTIFICATION_PULSE_MS, resolvedPulse)
      if (phoneBatteryPct != null && phoneBatteryPct in 0..100) {
        editor.putInt(KEY_PHONE_BATTERY_PCT, phoneBatteryPct)
      }
      editor.apply()
    }
  }

  suspend fun pullCachedTelemetryFromDataLayer(context: Context) = withContext(Dispatchers.IO) {
    runCatching {
      val appCtx = context.applicationContext
      val dataClient = Wearable.getDataClient(appCtx)
      val telemetryItems = dataClient.getDataItems(Uri.parse("wear://*/chronocanvas/telemetry")).awaitTask()
      var latestTs = -1L
      var bestUnread: Int? = null
      var bestPulse: Long? = null
      var bestPhoneBat: Int? = null
      try {
        for (item in telemetryItems) {
          val map = DataMapItem.fromDataItem(item).dataMap
          val ts = map.getLong("timestamp_ms", 0L)
          if (ts >= latestTs) {
            latestTs = ts
            bestUnread = map.getInt("unread_count", 0)
            bestPulse = map.getLong("pulse_ms", 0L)
            bestPhoneBat = map.getInt("phone_battery_pct", -1)
          }
        }
      } finally {
        telemetryItems.release()
      }

      if (bestUnread != null) {
        updateNotificationTelemetry(
          unreadCount = bestUnread,
          pulseMs = bestPulse ?: 0L,
          context = appCtx,
          phoneBatteryPct = bestPhoneBat?.takeIf { it in 0..100 },
        )
        return@runCatching
      }

      val notifItems = dataClient.getDataItems(Uri.parse("wear://*/chronocanvas/notifications")).awaitTask()
      var notifTs = -1L
      var notifUnread: Int? = null
      var notifPulse = 0L
      try {
        for (item in notifItems) {
          val map = DataMapItem.fromDataItem(item).dataMap
          val ts = map.getLong("timestamp_ms", 0L)
          if (ts >= notifTs) {
            notifTs = ts
            notifUnread = map.getInt("unread_count", 0)
            notifPulse = map.getLong("pulse_ms", 0L)
          }
        }
      } finally {
        notifItems.release()
      }
      if (notifUnread != null) {
        updateNotificationTelemetry(
          unreadCount = notifUnread,
          pulseMs = notifPulse,
          context = appCtx,
        )
      }
    }
  }

  fun readWatchBatteryPct(context: Context): Int {
    val status = context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    if (level >= 0 && scale > 0) {
      return ((level * 100f) / scale).toInt().coerceIn(0, 100)
    }
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    return if (capacity in 0..100) capacity else 85
  }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
  addOnSuccessListener { result ->
    if (cont.isActive) cont.resume(result)
  }
  addOnFailureListener { exc ->
    if (cont.isActive) cont.resumeWithException(exc)
  }
}
