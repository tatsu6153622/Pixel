package com.aistudio.chronocanvas.wear

import android.content.Context
import android.os.Build
import com.aistudio.chronocanvas.shared.codec.WatchConfigCodec
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WatchStatusReporter {

  const val PATH_WATCH_STATUS = "/chronocanvas/watch_status"
  const val PATH_WATCH_STATUS_REPLY = "/chronocanvas/watch_status_reply"
  const val PATH_REQUEST_TELEMETRY = "/chronocanvas/request_telemetry"

  suspend fun publishStatusToPhone(context: Context) = withContext(Dispatchers.IO) {
    runCatching {
      val now = System.currentTimeMillis()
      val versionCode = BuildConfig.VERSION_CODE
      val versionName = BuildConfig.VERSION_NAME
      val schemaVersion = WatchConfigCodec.SCHEMA_VERSION
      val model = Build.MODEL ?: "Pixel Watch 3"

      val putReq = PutDataMapRequest.create(PATH_WATCH_STATUS).apply {
        dataMap.putInt("version_code", versionCode)
        dataMap.putString("version_name", versionName)
        dataMap.putInt("schema_version", schemaVersion)
        dataMap.putString("device_model", model)
        dataMap.putLong("timestamp_ms", now)
      }.asPutDataRequest().setUrgent()

      Wearable.getDataClient(context).putDataItem(putReq)

      val payload = "$versionCode|$versionName|$schemaVersion|$model|$now".toByteArray(Charsets.UTF_8)
      Wearable.getNodeClient(context).connectedNodes
        .addOnSuccessListener { nodes ->
          val msgClient = Wearable.getMessageClient(context)
          nodes.forEach { node ->
            msgClient.sendMessage(node.id, PATH_WATCH_STATUS_REPLY, payload)
            msgClient.sendMessage(node.id, PATH_REQUEST_TELEMETRY, ByteArray(0))
          }
        }
    }
  }

  suspend fun requestTelemetryFromPhone(context: Context) = withContext(Dispatchers.IO) {
    runCatching {
      Wearable.getNodeClient(context).connectedNodes
        .addOnSuccessListener { nodes ->
          val msgClient = Wearable.getMessageClient(context)
          nodes.forEach { node ->
            msgClient.sendMessage(node.id, PATH_REQUEST_TELEMETRY, ByteArray(0))
          }
        }
    }
  }
}
