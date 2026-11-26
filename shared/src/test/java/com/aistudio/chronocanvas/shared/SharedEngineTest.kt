package com.aistudio.chronocanvas.shared

import com.aistudio.chronocanvas.shared.battery.BatteryOptimizerEngine
import com.aistudio.chronocanvas.shared.codec.WatchConfigCodec
import com.aistudio.chronocanvas.shared.model.BatteryProfile
import com.aistudio.chronocanvas.shared.model.ClockStyleConfig
import com.aistudio.chronocanvas.shared.model.ClockStyleId
import com.aistudio.chronocanvas.shared.model.CustomWidgetExtension
import com.aistudio.chronocanvas.shared.model.LauncherSettingsConfig
import com.aistudio.chronocanvas.shared.model.NotificationAlertConfig
import com.aistudio.chronocanvas.shared.model.NotificationEdgeStyle
import com.aistudio.chronocanvas.shared.model.ShaderPresetId
import com.aistudio.chronocanvas.shared.model.WallpaperAutomationConfig
import com.aistudio.chronocanvas.shared.model.WallpaperConfig
import com.aistudio.chronocanvas.shared.model.WallpaperSourceType
import com.aistudio.chronocanvas.shared.model.WatchHomeConfig
import com.aistudio.chronocanvas.shared.model.WatchTelemetrySnapshot
import com.aistudio.chronocanvas.shared.model.WidgetSlotConfig
import com.aistudio.chronocanvas.shared.model.WidgetSlotPosition
import com.aistudio.chronocanvas.shared.registry.PLUGIN_NONE
import com.aistudio.chronocanvas.shared.registry.WatchExtensionRegistry
import com.aistudio.chronocanvas.shared.ui.resolveEffectiveWallpaper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedEngineTest {

  @Test
  fun codecRoundTripPreservesFinePositioningGlowAndAutomation() {
    val customExt = CustomWidgetExtension(
      pluginId = "ext_caffeine",
      title = "カフェイン",
      shortTag = "CAF",
      defaultValue = "120",
      unit = "mg",
      iconKey = "spark",
    )
    val original = WatchHomeConfig(
      id = "test_cfg",
      name = "Test Watch",
      showAppDockButton = false,
      clockStyle = ClockStyleConfig(
        styleId = ClockStyleId.DATA_MATRIX,
        accentColorHex = 0xFFFFB84D,
        dateTextColorHex = 0xFFFF6B7A,
        offsetXFraction = 0.05f,
        offsetYFraction = -0.10f,
        showModelTag = false,
      ),
      notificationAlert = NotificationAlertConfig(
        edgeGlowEnabled = true,
        edgeGlowStyle = NotificationEdgeStyle.ORBIT_SWEEP,
        flashDurationSec = 10,
      ),
      wallpaperAutomation = WallpaperAutomationConfig(
        switchOnOffWrist = true,
        offWristShaderPreset = ShaderPresetId.PURE_OLED_CARBON,
        timeScheduleEnabled = true,
      ),
      launcherSettings = LauncherSettingsConfig(
        keepAliveHomeMode = false,
        ambientAlwaysOnEnabled = true,
        autoLaunchOnBoot = false,
        showOngoingWatchBadge = true,
      ),
      customExtensions = listOf(customExt),
      widgetSlots = listOf(
        WidgetSlotConfig(
          position = WidgetSlotPosition.TOP_ARC,
          pluginId = PLUGIN_NONE,
          offsetXFraction = 0.12f,
          offsetYFraction = -0.22f,
        ),
      ),
    )

    val json = WatchConfigCodec.toJson(original)
    val decoded = WatchConfigCodec.fromJson(json)

    assertEquals(original.id, decoded.id)
    assertFalse(decoded.showAppDockButton)
    assertEquals(ClockStyleId.DATA_MATRIX, decoded.clockStyle.styleId)
    assertEquals(0xFFFF6B7A, decoded.clockStyle.dateTextColorHex)
    assertFalse(decoded.clockStyle.showModelTag)
    assertEquals(0.05f, decoded.clockStyle.offsetXFraction, 0.001f)
    assertEquals(NotificationEdgeStyle.ORBIT_SWEEP, decoded.notificationAlert.edgeGlowStyle)
    assertTrue(decoded.wallpaperAutomation.switchOnOffWrist)
    assertTrue(decoded.wallpaperAutomation.timeScheduleEnabled)
    assertFalse(decoded.launcherSettings.keepAliveHomeMode)
    assertTrue(decoded.launcherSettings.ambientAlwaysOnEnabled)
    assertFalse(decoded.launcherSettings.autoLaunchOnBoot)
    assertEquals(PLUGIN_NONE, decoded.widgetSlots.first().pluginId)
    assertEquals(0.12f, decoded.widgetSlots.first().offsetXFraction, 0.001f)
  }

  @Test
  fun resolveEffectiveWallpaperSwitchesOnOffWristAndTimeSchedule() {
    val config = WatchHomeConfig(
      wallpaper = WallpaperConfig(
        sourceType = WallpaperSourceType.LOCAL_VIDEO_URI,
        videoUriString = "file:///main.mp4",
      ),
      wallpaperAutomation = WallpaperAutomationConfig(
        switchOnOffWrist = true,
        offWristSourceType = WallpaperSourceType.PROCEDURAL_SHADER,
        offWristShaderPreset = ShaderPresetId.PURE_OLED_CARBON,
        timeScheduleEnabled = true,
      ),
    )

    val offWristWp = resolveEffectiveWallpaper(
      config = config,
      telemetry = WatchTelemetrySnapshot(isOffWrist = true),
      hour24 = 14,
    )
    assertEquals(WallpaperSourceType.PROCEDURAL_SHADER, offWristWp.sourceType)
    assertEquals(ShaderPresetId.PURE_OLED_CARBON, offWristWp.shaderPresetId)

    val eveningWp = resolveEffectiveWallpaper(
      config = config,
      telemetry = WatchTelemetrySnapshot(isOffWrist = false),
      hour24 = 20,
    )
    assertEquals(WallpaperSourceType.PRESET_ART, eveningWp.sourceType)
    assertEquals("preset_horology", eveningWp.presetArtId)
  }

  @Test
  fun batteryOptimizerUltraEnduranceOutlastsMaxVisualForVideo() {
    val baseVideo = WatchHomeConfig(
      wallpaper = WallpaperConfig(
        sourceType = WallpaperSourceType.LOCAL_VIDEO_URI,
        videoUriString = "file:///sample.mp4",
      ),
    )
    val ultra = BatteryOptimizerEngine.applyProfile(baseVideo, BatteryProfile.ULTRA_ENDURANCE)
    val maxVis = BatteryOptimizerEngine.applyProfile(baseVideo, BatteryProfile.MAX_VISUAL)

    val ultraReport = BatteryOptimizerEngine.analyze(ultra)
    val maxReport = BatteryOptimizerEngine.analyze(maxVis)

    assertTrue(ultraReport.estimatedHours > maxReport.estimatedHours)
    assertTrue(ultraReport.efficiencyScore > maxReport.efficiencyScore)
  }

  @Test
  fun notificationPluginResolvesUnreadCount() {
    val plugin = WatchExtensionRegistry.findPlugin("sys_notifications")
    val resolved = plugin.resolveDisplay(
      slot = WidgetSlotConfig(WidgetSlotPosition.TOP_ARC, "sys_notifications"),
      telemetry = WatchTelemetrySnapshot(unreadNotificationCount = 5),
    )
    assertEquals("通知", resolved.title)
    assertEquals("5件", resolved.primaryValue)
  }
}
