package com.aistudio.chronocanvas.wear

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Handles Watch boot/power-on and package-update events so ChronoCanvas can restore itself
 * or surface a high-priority 1-tap launcher card if Android 14+ blocks background activity launch.
 */
class WatchBootReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
      return
    }

    WatchLocalStore.initialize(context)
    val launcherCfg = WatchLocalStore.configState.value.launcherSettings
    if (!launcherCfg.autoLaunchOnBoot) return

    val launchIntent = Intent(context, WatchHomeActivity::class.java).apply {
      addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
          Intent.FLAG_ACTIVITY_SINGLE_TOP or
          Intent.FLAG_ACTIVITY_CLEAR_TOP,
      )
    }

    // Works directly when set as default HOME launcher or permitted by OEM policy
    runCatching {
      context.startActivity(launchIntent)
    }

    // Also post a tap-to-open launcher shortcut notification as a reliable fallback on Wear OS 5
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    WatchLauncherService.ensureNotificationChannel(context)
    val pendingIntent = PendingIntent.getActivity(
      context,
      202,
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, WatchLauncherService.CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_watch_ongoing)
      .setContentTitle("ChronoCanvas ホームを開く")
      .setContentText("タップしてカスタム壁紙・時計を起動します")
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .build()

    context.getSystemService(NotificationManager::class.java)
      ?.notify(BOOT_NOTIFICATION_ID, notification)
  }

  companion object {
    private const val BOOT_NOTIFICATION_ID = 3002
  }
}
