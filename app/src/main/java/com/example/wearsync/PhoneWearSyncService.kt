package com.example.wearsync

import com.example.notifications.ChronoNotificationListenerService
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WatchAppStatus(
  val versionCode: Int,
  val versionName: String,
  val schemaVersion: Int,
  val deviceModel: String,
  val lastSeenEpochMs: Long,
)

object WatchLiveBridgeState {
  private val _watchStatus = MutableStateFlow<WatchAppStatus?>(null)
  val watchStatus: StateFlow<WatchAppStatus?> = _watchStatus.asStateFlow()

  private val _autoSyncTrigger = MutableStateFlow(0)
  val autoSyncTrigger: StateFlow<Int> = _autoSyncTrigger.asStateFlow()

  fun updateStatus(status: WatchAppStatus) {
    _watchStatus.value = status
  }

  fun requestAutoSyncOnConnect() {
    _autoSyncTrigger.value = _autoSyncTrigger.value + 1
  }
}

class PhoneWearSyncService : WearableListenerService() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
    if (capabilityInfo.name == WearSyncManager.CAPABILITY_WATCH_HOME &&
      capabilityInfo.nodes.isNotEmpty()
    ) {
      WatchLiveBridgeState.requestAutoSyncOnConnect()
      scope.launch {
        ChronoNotificationListenerService.syncCurrentActiveNotificationsToWatch(applicationContext)
      }
    }
  }

  override fun onDataChanged(dataEvents: DataEventBuffer) {
    for (event in dataEvents) {
      if (event.type == DataEvent.TYPE_CHANGED &&
        event.dataItem.uri.path == WearSyncManager.PATH_WATCH_STATUS
      ) {
        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
        WatchLiveBridgeState.updateStatus(
          WatchAppStatus(
            versionCode = dataMap.getInt("version_code", 1),
            versionName = dataMap.getString("version_name", "1.0"),
            schemaVersion = dataMap.getInt("schema_version", 1),
            deviceModel = dataMap.getString("device_model", "Pixel Watch 3"),
            lastSeenEpochMs = dataMap.getLong("timestamp_ms", System.currentTimeMillis()),
          ),
        )
        scope.launch {
          ChronoNotificationListenerService.syncCurrentActiveNotificationsToWatch(applicationContext)
        }
      }
    }
  }

  override fun onMessageReceived(messageEvent: MessageEvent) {
    when (messageEvent.path) {
      WearSyncManager.PATH_WATCH_STATUS_REPLY -> {
        val parts = String(messageEvent.data, Charsets.UTF_8).split("|")
        val versionCode = parts.getOrNull(0)?.toIntOrNull() ?: return
        val versionName = parts.getOrNull(1).orEmpty().ifBlank { "$versionCode.0" }
        val schemaVersion = parts.getOrNull(2)?.toIntOrNull() ?: 1
        val deviceModel = parts.getOrNull(3).orEmpty().ifBlank { "Pixel Watch 3" }
        val timestamp = parts.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis()
        WatchLiveBridgeState.updateStatus(
          WatchAppStatus(
            versionCode = versionCode,
            versionName = versionName,
            schemaVersion = schemaVersion,
            deviceModel = deviceModel,
            lastSeenEpochMs = timestamp,
          ),
        )
        scope.launch {
          ChronoNotificationListenerService.syncCurrentActiveNotificationsToWatch(applicationContext)
        }
      }
      WearSyncManager.PATH_REQUEST_TELEMETRY -> {
        scope.launch {
          ChronoNotificationListenerService.syncCurrentActiveNotificationsToWatch(applicationContext)
        }
      }
    }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }
}
