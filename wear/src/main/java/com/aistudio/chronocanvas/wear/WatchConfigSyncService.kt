package com.aistudio.chronocanvas.wear

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WatchConfigSyncService : WearableListenerService() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onDataChanged(dataEvents: DataEventBuffer) {
    for (event in dataEvents) {
      if (event.type == DataEvent.TYPE_CHANGED) {
        when (event.dataItem.uri.path) {
          "/chronocanvas/config" -> {
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val json = dataMap.getString("config_json")
            if (!json.isNullOrBlank()) {
              WatchLocalStore.saveConfigJson(applicationContext, json)
              WatchLauncherService.syncServiceState(
                applicationContext,
                WatchLocalStore.configState.value.launcherSettings.keepAliveHomeMode,
              )
              scope.launch { WatchStatusReporter.publishStatusToPhone(applicationContext) }
            }
          }
          "/chronocanvas/telemetry" -> {
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val count = dataMap.getInt("unread_count", 0)
            val pulseMs = dataMap.getLong("pulse_ms", 0L)
            val phoneBat = dataMap.getInt("phone_battery_pct", -1).takeIf { it in 0..100 }
            WatchLocalStore.updateNotificationTelemetry(
              unreadCount = count,
              pulseMs = pulseMs,
              context = applicationContext,
              phoneBatteryPct = phoneBat,
            )
          }
          "/chronocanvas/notifications" -> {
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val count = dataMap.getInt("unread_count", 0)
            val pulseMs = dataMap.getLong("pulse_ms", 0L)
            WatchLocalStore.updateNotificationTelemetry(
              unreadCount = count,
              pulseMs = pulseMs,
              context = applicationContext,
            )
          }
        }
      }
    }
  }

  override fun onMessageReceived(messageEvent: MessageEvent) {
    when (messageEvent.path) {
      "/chronocanvas/apply_now" -> {
        val json = String(messageEvent.data, Charsets.UTF_8)
        if (json.isNotBlank()) {
          WatchLocalStore.saveConfigJson(applicationContext, json)
          WatchLauncherService.syncServiceState(
            applicationContext,
            WatchLocalStore.configState.value.launcherSettings.keepAliveHomeMode,
          )
          scope.launch { WatchStatusReporter.publishStatusToPhone(applicationContext) }
        }
      }
      "/chronocanvas/telemetry_sync" -> {
        val parts = String(messageEvent.data, Charsets.UTF_8).split("|")
        val count = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val pulseMs = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        val phoneBat = parts.getOrNull(2)?.toIntOrNull()?.takeIf { it in 0..100 }
        WatchLocalStore.updateNotificationTelemetry(
          unreadCount = count,
          pulseMs = pulseMs,
          context = applicationContext,
          phoneBatteryPct = phoneBat,
        )
      }
      "/chronocanvas/notification_pulse" -> {
        val parts = String(messageEvent.data, Charsets.UTF_8).split(":")
        val count = parts.firstOrNull()?.toIntOrNull() ?: 1
        val pulseMs = parts.getOrNull(1)?.toLongOrNull() ?: System.currentTimeMillis()
        WatchLocalStore.updateNotificationTelemetry(
          unreadCount = count,
          pulseMs = pulseMs,
          context = applicationContext,
        )
      }
      "/chronocanvas/request_status" -> {
        scope.launch { WatchStatusReporter.publishStatusToPhone(applicationContext) }
      }
      "/chronocanvas/force_refresh" -> {
        WatchLocalStore.initialize(applicationContext)
        scope.launch {
          WatchLocalStore.pullCachedTelemetryFromDataLayer(applicationContext)
          WatchStatusReporter.publishStatusToPhone(applicationContext)
        }
      }
      "/chronocanvas/launch_home" -> {
        WatchLocalStore.initialize(applicationContext)
        WatchLauncherService.syncServiceState(
          applicationContext,
          WatchLocalStore.configState.value.launcherSettings.keepAliveHomeMode,
        )
        scope.launch {
          WatchLocalStore.pullCachedTelemetryFromDataLayer(applicationContext)
          WatchStatusReporter.publishStatusToPhone(applicationContext)
        }
        runCatching {
          startActivity(
            Intent(applicationContext, WatchHomeActivity::class.java).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
          )
        }
      }
    }
  }

  override fun onChannelOpened(channel: ChannelClient.Channel) {
    val key = when (channel.path) {
      "/chronocanvas/video_wallpaper" -> "main"
      "/chronocanvas/video_offwrist" -> "offwrist"
      else -> null
    } ?: return

    val dir = File(filesDir, "wallpapers").apply { mkdirs() }
    val target = File(dir, "${key}_wallpaper.mp4")
    val client = Wearable.getChannelClient(this)
    scope.launch {
      try {
        suspendCancellableCoroutine<Unit> { cont ->
          client.receiveFile(channel, Uri.fromFile(target), false)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
        }
        WatchLocalStore.saveVideoFile(applicationContext, key, target)
      } catch (e: Exception) {
        Log.e("WatchConfigSyncService", "Failed receiving video wallpaper", e)
      }
    }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }
}
