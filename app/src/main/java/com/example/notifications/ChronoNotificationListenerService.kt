package com.example.notifications

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationBridgeState {
  private val _unreadCount = MutableStateFlow(0)
  val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

  private val _lastPulseMs = MutableStateFlow(0L)
  val lastPulseMs: StateFlow<Long> = _lastPulseMs.asStateFlow()

  fun updateFromSystem(count: Int, triggerPulse: Boolean) {
    _unreadCount.value = count.coerceAtLeast(0)
    if (triggerPulse) {
      _lastPulseMs.value = System.currentTimeMillis()
    }
  }

  fun isNotificationAccessEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
      ?: return false
    val cn = ComponentName(context, ChronoNotificationListenerService::class.java).flattenToString()
    return flat.split(":").any { it.equals(cn, ignoreCase = true) }
  }
}

class ChronoNotificationListenerService : NotificationListenerService() {

  override fun onListenerConnected() {
    super.onListenerConnected()
    refreshActiveCount(triggerPulse = false)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    super.onNotificationPosted(sbn)
    val isOngoing = sbn?.isOngoing == true
    refreshActiveCount(triggerPulse = !isOngoing)
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    super.onNotificationRemoved(sbn)
    refreshActiveCount(triggerPulse = false)
  }

  private fun refreshActiveCount(triggerPulse: Boolean) {
    val count = runCatching {
      activeNotifications?.count { !it.isOngoing } ?: 0
    }.getOrDefault(0)
    NotificationBridgeState.updateFromSystem(count, triggerPulse)
  }
}
