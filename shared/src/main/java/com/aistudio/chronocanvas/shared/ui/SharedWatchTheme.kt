package com.aistudio.chronocanvas.shared.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object SharedWatchPalette {
  val ObsidianBg = Color(0xFF07090E)
  val GraphiteSurface = Color(0xFF10151E)
  val ElevatedCard = Color(0xFF171E2B)
  val OutlineHairline = Color(0xFF283347)
  val CyanAccent = Color(0xFF3EF0D0)
  val AmberAccent = Color(0xFFFFB84D)
  val CoralAccent = Color(0xFFFF6B7A)
  val LavenderAccent = Color(0xFF9E8CFF)
  val TitaniumText = Color(0xFFF2F6FA)
  val SlateMuted = Color(0xFF8C9BB2)
  val PureBlackOled = Color(0xFF000000)

  val AccentSwatches: List<Pair<String, Long>> = listOf(
    "エレクトリックシアン" to 0xFF3EF0D0,
    "ホロジカルアンバー" to 0xFFFFB84D,
    "ソーラーコーラル" to 0xFFFF6B7A,
    "ネオンミント" to 0xFF5CF28E,
    "コスミックバイオレット" to 0xFF9E8CFF,
    "チタニウムホワイト" to 0xFFF2F6FA,
  )
}

fun resolvePluginIcon(iconKey: String): ImageVector = when (iconKey) {
  "none" -> Icons.Rounded.Block
  "notifications" -> Icons.Rounded.NotificationsActive
  "battery" -> Icons.Rounded.BatteryChargingFull
  "steps" -> Icons.Rounded.DirectionsRun
  "heart" -> Icons.Rounded.Favorite
  "weather" -> Icons.Rounded.WbSunny
  "calendar" -> Icons.Rounded.CalendarToday
  "music" -> Icons.Rounded.MusicNote
  "phone" -> Icons.Rounded.PhoneAndroid
  "fitness" -> Icons.Rounded.FitnessCenter
  "wallet" -> Icons.Rounded.Wallet
  "map" -> Icons.Rounded.Map
  "timer" -> Icons.Rounded.Timer
  "flashlight" -> Icons.Rounded.FlashlightOn
  else -> Icons.Rounded.AutoAwesome
}
