package com.example.wearsync

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.aistudio.chronocanvas.shared.codec.WatchConfigCodec
import com.aistudio.chronocanvas.shared.model.WallpaperSourceType
import com.aistudio.chronocanvas.shared.model.WatchHomeConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class WearNodeInfo(
  val id: String,
  val displayName: String,
  val isNearby: Boolean,
  val hasCompanionApp: Boolean,
)

sealed interface WearSyncResult {
  data class SyncedToWatch(
    val nodeCount: Int,
    val primaryNodeName: String,
    val payloadBytes: Int,
    val videoTransferred: Boolean,
    val message: String,
  ) : WearSyncResult

  data class NoPairedWatchFound(
    val payloadBytes: Int,
    val reason: String,
  ) : WearSyncResult

  data class Error(
    val message: String,
  ) : WearSyncResult
}

class WearSyncManager(private val context: Context) {

  companion object {
    const val CAPABILITY_WATCH_HOME = "chronocanvas_watch_home"
    const val PATH_CONFIG = "/chronocanvas/config"
    const val PATH_APPLY_NOW = "/chronocanvas/apply_now"
    const val PATH_VIDEO_WALLPAPER = "/chronocanvas/video_wallpaper"
    const val PATH_VIDEO_OFFWRIST = "/chronocanvas/video_offwrist"
    const val PATH_NOTIFICATIONS = "/chronocanvas/notifications"
    const val PATH_NOTIFICATION_PULSE = "/chronocanvas/notification_pulse"
    const val PATH_WATCH_STATUS = "/chronocanvas/watch_status"
    const val PATH_WATCH_STATUS_REPLY = "/chronocanvas/watch_status_reply"
    const val PATH_REQUEST_STATUS = "/chronocanvas/request_status"
    const val PATH_FORCE_REFRESH = "/chronocanvas/force_refresh"
  }

  private var lastSyncedMainVideoUri: String? = null
  private var lastSyncedOffWristVideoUri: String? = null

  private fun isPlayServicesWearableAvailable(): Boolean {
    val availability = GoogleApiAvailability.getInstance()
    val status = availability.isGooglePlayServicesAvailable(context)
    return status == ConnectionResult.SUCCESS
  }

  suspend fun scanConnectedNodes(): Pair<Boolean, List<WearNodeInfo>> = withContext(Dispatchers.IO) {
    if (!isPlayServicesWearableAvailable()) {
      return@withContext false to emptyList()
    }
    return@withContext try {
      val dataClient = Wearable.getDataClient(context)
      runCatching { GoogleApiAvailability.getInstance().checkApiAvailability(dataClient).awaitTask() }
        .getOrElse { return@withContext false to emptyList() }

      val nodes = Wearable.getNodeClient(context).connectedNodes.awaitTask()
      val capInfo = runCatching {
        Wearable.getCapabilityClient(context)
          .getCapability(CAPABILITY_WATCH_HOME, CapabilityClient.FILTER_REACHABLE)
          .awaitTask()
      }.getOrNull()
      val capNodeIds = capInfo?.nodes?.map { it.id }?.toSet().orEmpty()

      val list = nodes.map { node ->
        WearNodeInfo(
          id = node.id,
          displayName = node.displayName,
          isNearby = node.isNearby,
          hasCompanionApp = node.id in capNodeIds,
        )
      }
      true to list
    } catch (_: Exception) {
      false to emptyList()
    }
  }

  suspend fun queryWatchAppStatus(nodes: List<WearNodeInfo>): WatchAppStatus? = withContext(Dispatchers.IO) {
    if (!isPlayServicesWearableAvailable()) return@withContext null
    runCatching {
      val items = Wearable.getDataClient(context)
        .getDataItems(Uri.parse("wear://*$PATH_WATCH_STATUS"))
        .awaitTask()
      var latest: WatchAppStatus? = null
      try {
        for (item in items) {
          val map = DataMapItem.fromDataItem(item).dataMap
          val candidate = WatchAppStatus(
            versionCode = map.getInt("version_code", 1),
            versionName = map.getString("version_name", "1.0"),
            schemaVersion = map.getInt("schema_version", 1),
            deviceModel = map.getString("device_model", "Pixel Watch 3"),
            lastSeenEpochMs = map.getLong("timestamp_ms", System.currentTimeMillis()),
          )
          if (latest == null || candidate.lastSeenEpochMs >= latest.lastSeenEpochMs) {
            latest = candidate
          }
        }
      } finally {
        items.release()
      }
      latest?.let { WatchLiveBridgeState.updateStatus(it) }

      if (nodes.isNotEmpty()) {
        val msgClient = Wearable.getMessageClient(context)
        nodes.forEach { node ->
          runCatching {
            msgClient.sendMessage(node.id, PATH_REQUEST_STATUS, ByteArray(0)).awaitTask()
          }
        }
      }
      latest
    }.getOrNull()
  }

  suspend fun sendForceRefreshToWatch(): Result<String> = withContext(Dispatchers.IO) {
    if (!isPlayServicesWearableAvailable()) {
      return@withContext Result.failure(IllegalStateException("Wear OS Data Layerが利用できません"))
    }
    return@withContext runCatching {
      val (_, nodes) = scanConnectedNodes()
      if (nodes.isEmpty()) {
        throw IllegalStateException("接続中のPixel Watch 3が見つかりません")
      }
      val msgClient = Wearable.getMessageClient(context)
      nodes.forEach { node ->
        msgClient.sendMessage(node.id, PATH_FORCE_REFRESH, ByteArray(0)).awaitTask()
      }
      "Pixel Watch 3へ強制リフレッシュ＆バージョン確認を送信しました"
    }
  }

  suspend fun launchRemoteWatchUpdate(
    nodeId: String? = null,
    customReleaseUrl: String? = null,
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!isPlayServicesWearableAvailable()) {
      return@withContext Result.failure(IllegalStateException("Wear OS接続が利用できない端末です"))
    }
    val rawUrl = customReleaseUrl?.trim().orEmpty()
    val targetUri = if (rawUrl.isNotBlank()) {
      val parsed = runCatching { Uri.parse(rawUrl) }.getOrNull()
      val scheme = parsed?.scheme?.lowercase().orEmpty()
      if (parsed == null || scheme !in listOf("https", "http", "market")) {
        return@withContext Result.failure(
          IllegalArgumentException("URLは https:// または market:// で入力してください"),
        )
      }
      parsed
    } else {
      Uri.parse("market://details?id=${context.packageName}")
    }

    return@withContext runCatching {
      val (_, nodes) = scanConnectedNodes()
      if (nodes.isEmpty()) {
        throw IllegalStateException("接続中のPixel Watch 3が検出されませんでした")
      }
      val resolvedNodeId = nodeId
        ?: nodes.firstOrNull { it.hasCompanionApp }?.id
        ?: nodes.first().id

      val intent = Intent(Intent.ACTION_VIEW)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setData(targetUri)

      val helper = RemoteActivityHelper(context)
      helper.startRemoteActivity(intent, resolvedNodeId).awaitFuture()

      val targetName = nodes.firstOrNull { it.id == resolvedNodeId }?.displayName ?: "Pixel Watch 3"
      if (rawUrl.isBlank()) {
        "$targetName 側でGoogle Playの更新ページを開きました"
      } else {
        "$targetName 側で配布リンクを開きました"
      }
    }
  }

  suspend fun pushNotificationTelemetry(unreadCount: Int, pulseMs: Long) = withContext(Dispatchers.IO) {
    if (!isPlayServicesWearableAvailable()) return@withContext
    runCatching {
      val (_, nodes) = scanConnectedNodes()
      if (nodes.isEmpty()) return@withContext
      val putReq = PutDataMapRequest.create(PATH_NOTIFICATIONS).apply {
        dataMap.putInt("unread_count", unreadCount)
        dataMap.putLong("pulse_ms", pulseMs)
      }.asPutDataRequest().setUrgent()
      Wearable.getDataClient(context).putDataItem(putReq).awaitTask()

      val payload = "$unreadCount:$pulseMs".toByteArray(Charsets.UTF_8)
      val messageClient = Wearable.getMessageClient(context)
      nodes.forEach { node ->
        runCatching { messageClient.sendMessage(node.id, PATH_NOTIFICATION_PULSE, payload).awaitTask() }
      }
    }
  }

  suspend fun syncConfigToWatch(
    config: WatchHomeConfig,
    forceResendVideos: Boolean = true,
  ): WearSyncResult = withContext(Dispatchers.IO) {
    val stamped = config.copy(updatedAtEpochMs = System.currentTimeMillis())
    val json = WatchConfigCodec.toJson(stamped)
    val bytes = json.toByteArray(Charsets.UTF_8)

    if (!isPlayServicesWearableAvailable()) {
      return@withContext WearSyncResult.NoPairedWatchFound(
        payloadBytes = bytes.size,
        reason = "この端末/エミュレータではGoogle Play開発者サービスのWear OS Data Layerが利用できません。実機ペアリング後に同期できます。",
      )
    }

    return@withContext try {
      val (_, nodes) = scanConnectedNodes()
      if (nodes.isEmpty()) {
        return@withContext WearSyncResult.NoPairedWatchFound(
          payloadBytes = bytes.size,
          reason = "ペアリング済みのPixel Watch 3ノードが検出されませんでした（設定ペイロード ${bytes.size} bytes の生成と検証は完了しています）。",
        )
      }

      val putReq = PutDataMapRequest.create(PATH_CONFIG).apply {
        dataMap.putString("config_json", json)
        dataMap.putLong("timestamp", stamped.updatedAtEpochMs)
      }.asPutDataRequest().setUrgent()

      Wearable.getDataClient(context).putDataItem(putReq).awaitTask()

      val messageClient = Wearable.getMessageClient(context)
      nodes.forEach { node ->
        runCatching {
          messageClient.sendMessage(node.id, PATH_APPLY_NOW, bytes).awaitTask()
        }
      }

      var videoTransferred = false
      val targetNode = nodes.firstOrNull { it.hasCompanionApp } ?: nodes.first()
      val channelClient = Wearable.getChannelClient(context)

      val mainUriStr = stamped.wallpaper.videoUriString
      val shouldSendMainVideo = stamped.wallpaper.sourceType == WallpaperSourceType.LOCAL_VIDEO_URI &&
        mainUriStr.isNotBlank() &&
        (forceResendVideos || mainUriStr != lastSyncedMainVideoUri)

      if (shouldSendMainVideo) {
        val videoUri = Uri.parse(mainUriStr)
        val channel = channelClient.openChannel(targetNode.id, PATH_VIDEO_WALLPAPER).awaitTask()
        channelClient.sendFile(channel, videoUri).awaitTask()
        lastSyncedMainVideoUri = mainUriStr
        videoTransferred = true
      }

      val offUriStr = stamped.wallpaperAutomation.offWristVideoUriString
      val shouldSendOffVideo = stamped.wallpaperAutomation.switchOnOffWrist &&
        stamped.wallpaperAutomation.offWristSourceType == WallpaperSourceType.LOCAL_VIDEO_URI &&
        offUriStr.isNotBlank() &&
        (forceResendVideos || offUriStr != lastSyncedOffWristVideoUri)

      if (shouldSendOffVideo) {
        val offUri = Uri.parse(offUriStr)
        val offChannel = channelClient.openChannel(targetNode.id, PATH_VIDEO_OFFWRIST).awaitTask()
        channelClient.sendFile(offChannel, offUri).awaitTask()
        lastSyncedOffWristVideoUri = offUriStr
        videoTransferred = true
      }

      val primary = nodes.first()
      WearSyncResult.SyncedToWatch(
        nodeCount = nodes.size,
        primaryNodeName = primary.displayName,
        payloadBytes = bytes.size,
        videoTransferred = videoTransferred,
        message = buildString {
          append("${primary.displayName} へOTA反映(${bytes.size}B)を完了しました。")
          if (videoTransferred) append(" 動画壁紙ファイルもChannelClientで転送済みです。")
        },
      )
    } catch (e: Exception) {
      WearSyncResult.Error("Wear OS同期エラー: ${e.localizedMessage ?: "不明なエラー"}")
    }
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

private suspend fun <T> ListenableFuture<T>.awaitFuture(): T = suspendCancellableCoroutine { cont ->
  addListener(
    {
      try {
        if (cont.isActive) cont.resume(get())
      } catch (e: Exception) {
        if (cont.isActive) cont.resumeWithException(e.cause ?: e)
      }
    },
    Runnable::run,
  )
}
