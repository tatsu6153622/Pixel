package com.aistudio.chronocanvas.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

/**
 * Keeps ChronoCanvas pinned across Wear OS 5 ambient timeouts via OngoingActivity
 * and provides a 1-tap return icon on the system watch face if the user navigates away.
 */
class WatchLauncherService : Service() {

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }

    ensureNotificationChannel(this)
    val notification = buildOngoingNotification(this)
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    }.onFailure {
      stopSelf()
    }
    return START_STICKY
  }

  companion object {
    const val CHANNEL_ID = "chronocanvas_home_launcher"
    const val NOTIFICATION_ID = 3001
    const val ACTION_STOP = "com.aistudio.chronocanvas.wear.ACTION_STOP_LAUNCHER_SERVICE"

    fun syncServiceState(context: Context, enabled: Boolean) {
      if (!enabled) {
        context.stopService(Intent(context, WatchLauncherService::class.java))
        return
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
          context,
          android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
      }
      runCatching {
        ContextCompat.startForegroundService(
          context,
          Intent(context, WatchLauncherService::class.java),
        )
      }
    }

    fun ensureNotificationChannel(context: Context) {
      val manager = context.getSystemService(NotificationManager::class.java) ?: return
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "常駐ホームランチャー",
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = "画面消灯後もChronoCanvasのカスタム時計・動画壁紙を保持します"
          setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
      }
    }

    fun buildOngoingNotification(context: Context): Notification {
      val launchIntent = Intent(context, WatchHomeActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
      val touchIntent = PendingIntent.getActivity(
        context,
        101,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_watch_ongoing)
        .setContentTitle("ChronoCanvas ホーム稼働中")
        .setContentText("タップして動画壁紙・時計を表示")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(touchIntent)

      val status = Status.Builder()
        .addTemplate("ChronoCanvas ホーム")
        .build()

      val ongoingActivity = OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
        .setStaticIcon(R.drawable.ic_watch_ongoing)
        .setTouchIntent(touchIntent)
        .setStatus(status)
        .build()

      ongoingActivity.apply(context)
      return builder.build()
    }
  }
}
