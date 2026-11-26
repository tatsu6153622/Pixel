package com.example.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.wearsync.WearSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object PhoneTelemetryHelper {
  private const val PREFS_NAME = "chronocanvas_phone_telemetry"
  private const val KEY_UNREAD_COUNT = "unread_count"
  private const val KEY_PULSE_MS = "pulse_ms"

  fun readPhoneBatteryPct(context: Context): Int {
    val intent = context.applicationContext.registerReceiver(
      null,
      IntentFilter(Intent.ACTION_BATTERY_CHANGED),
    )
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    if (level >= 0 && scale > 0) {
      return ((level * 100f) / scale).toInt().coerceIn(0, 100)
    }
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    return if (capacity in 0..100) capacity else 92
  }

  fun saveNotificationSnapshot(context: Context, unreadCount: Int, pulseMs: Long) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putInt(KEY_UNREAD_COUNT, unreadCount.coerceAtLeast(0))
      .putLong(KEY_PULSE_MS, pulseMs)
      .apply()
  }

  fun loadNotificationSnapshot(context: Context): Pair<Int, Long> {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val count = prefs.getInt(KEY_UNREAD_COUNT, 0).coerceAtLeast(0)
    val pulseMs = prefs.getLong(KEY_PULSE_MS, 0L)
    return count to pulseMs
  }
}

object NotificationBridgeState {
  private val _unreadCount = MutableStateFlow(0)
  val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

  private val _lastPulseMs = MutableStateFlow(0L)
  val lastPulseMs: StateFlow<Long> = _lastPulseMs.asStateFlow()

  private val _isListenerConnected = MutableStateFlow(false)
  val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

  fun initialize(context: Context) {
    val (savedCount, savedPulseMs) = PhoneTelemetryHelper.loadNotificationSnapshot(context)
    if (_unreadCount.value == 0 && savedCount > 0) {
      _unreadCount.value = savedCount
      _lastPulseMs.value = savedPulseMs
    }
  }

  fun setListenerConnected(connected: Boolean) {
    _isListenerConnected.value = connected
  }

  fun updateFromSystem(
    count: Int,
    triggerPulse: Boolean,
    context: Context? = null,
  ): Long {
    val safeCount = count.coerceAtLeast(0)
    _unreadCount.value = safeCount
    val nextPulseMs = if (triggerPulse) {
      System.currentTimeMillis().also { _lastPulseMs.value = it }
    } else if (safeCount == 0) {
      0L.also { _lastPulseMs.value = 0L }
    } else {
      _lastPulseMs.value
    }
    if (context != null) {
      PhoneTelemetryHelper.saveNotificationSnapshot(context, safeCount, nextPulseMs)
    }
    return nextPulseMs
  }

  fun isNotificationAccessEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
      ?: return false
    val cn = ComponentName(context, ChronoNotificationListenerService::class.java).flattenToString()
    return flat.split(":").any { it.equals(cn, ignoreCase = true) }
  }
}

class ChronoNotificationListenerService : NotificationListenerService() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  companion object {
    @Volatile
    private var activeService: ChronoNotificationListenerService? = null

    fun requestRebindIfNeeded(context: Context) {
      val appCtx = context.applicationContext
      if (!NotificationBridgeState.isNotificationAccessEnabled(appCtx)) return
      if (activeService == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        runCatching {
          requestRebind(ComponentName(appCtx, ChronoNotificationListenerService::class.java))
        }
      }
    }

    suspend fun syncCurrentActiveNotificationsToWatch(context: Context) {
      val appCtx = context.applicationContext
      NotificationBridgeState.initialize(appCtx)
      val service = activeService
      if (service != null) {
        service.refreshActiveCountAndPush(triggerPulse = false)
      } else {
        requestRebindIfNeeded(appCtx)
        val count = NotificationBridgeState.unreadCount.value
        val pulseMs = NotificationBridgeState.lastPulseMs.value
        val phoneBat = PhoneTelemetryHelper.readPhoneBatteryPct(appCtx)
        WearSyncManager(appCtx).pushTelemetryToWatch(
          unreadCount = count,
          pulseMs = pulseMs,
          phoneBatteryPct = phoneBat,
        )
      }
    }

    fun countCountableNotifications(
      notifications: Array<StatusBarNotification>?,
      selfPackage: String,
    ): Int {
      if (notifications.isNullOrEmpty()) return 0
      val countable = notifications.filter { sbn ->
        if (sbn.isOngoing) return@filter false
        val flags = sbn.notification?.flags ?: 0
        if ((flags and Notification.FLAG_ONGOING_EVENT) != 0) return@filter false
        if ((flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) return@filter false
        if (sbn.packageName == selfPackage) return@filter false
        true
      }
      val childGroups = countable
        .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 && !it.groupKey.isNullOrBlank() }
        .mapTo(hashSetOf()) { it.groupKey }
      return countable.count { sbn ->
        val isSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        !isSummary || sbn.groupKey !in childGroups
      }
    }
  }

  override fun onListenerConnected() {
    super.onListenerConnected()
    activeService = this
    NotificationBridgeState.setListenerConnected(true)
    scope.launch {
      refreshActiveCountAndPush(triggerPulse = false)
    }
  }

  override fun onListenerDisconnected() {
    if (activeService === this) {
      activeService = null
    }
    NotificationBridgeState.setListenerConnected(false)
    super.onListenerDisconnected()
  }

  override fun onDestroy() {
    if (activeService === this) {
      activeService = null
    }
    NotificationBridgeState.setListenerConnected(false)
    scope.cancel()
    super.onDestroy()
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    super.onNotificationPosted(sbn)
    val isSilent = sbn == null ||
      sbn.isOngoing ||
      ((sbn.notification?.flags ?: 0) and Notification.FLAG_GROUP_SUMMARY) != 0 ||
      ((sbn.notification?.flags ?: 0) and Notification.FLAG_FOREGROUND_SERVICE) != 0 ||
      sbn.packageName == packageName
    scope.launch {
      refreshActiveCountAndPush(triggerPulse = !isSilent)
    }
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    super.onNotificationRemoved(sbn)
    scope.launch {
      refreshActiveCountAndPush(triggerPulse = false)
    }
  }

  private suspend fun refreshActiveCountAndPush(triggerPulse: Boolean) {
    val count = runCatching {
      countCountableNotifications(activeNotifications, packageName)
    }.getOrElse { NotificationBridgeState.unreadCount.value }

    val pulseMs = NotificationBridgeState.updateFromSystem(
      count = count,
      triggerPulse = triggerPulse,
      context = applicationContext,
    )
    val phoneBat = PhoneTelemetryHelper.readPhoneBatteryPct(applicationContext)
    WearSyncManager(applicationContext).pushTelemetryToWatch(
      unreadCount = count,
      pulseMs = pulseMs,
      phoneBatteryPct = phoneBat,
    )
  }
}
