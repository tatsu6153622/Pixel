package com.aistudio.chronocanvas.shared.model

enum class WatchModelSpec(
  val displayName: String,
  val nativePx: Int,
  val batteryCapacityMah: Int,
  val minLtpoHz: Int,
  val maxLtpoHz: Int,
) {
  PIXEL_WATCH_3_45MM(
    displayName = "Pixel Watch 3 (45mm)",
    nativePx = 456,
    batteryCapacityMah = 420,
    minLtpoHz = 1,
    maxLtpoHz = 60,
  ),
  PIXEL_WATCH_3_41MM(
    displayName = "Pixel Watch 3 (41mm)",
    nativePx = 408,
    batteryCapacityMah = 307,
    minLtpoHz = 1,
    maxLtpoHz = 60,
  ),
}

enum class ClockStyleId(
  val title: String,
  val subtitle: String,
) {
  CHRONO_ANALOG(
    title = "クロノ・アナログ",
    subtitle = "精密なスイープ秒針とルミナスインデックス",
  ),
  ORBIT_DIGITAL(
    title = "オービット・デジタル",
    subtitle = "太字スタック表示と軌道プログレスリング",
  ),
  MODULAR_MINIMAL(
    title = "モジュラー・ミニマル",
    subtitle = "LTPO 1Hz省電力に最適化された超軽量タイポグラフィ",
  ),
  DATA_MATRIX(
    title = "データ・マトリクス",
    subtitle = "モノスペース計器レイアウト（自由配置対応）",
  ),
}

enum class TickStyle(val label: String) {
  PRECISION_60("60分フル目盛り"),
  MINIMAL_12("12時間ドット"),
  QUARTER_4("4方位インデックス"),
  NONE("目盛りなし"),
}

enum class DateDisplayFormat(val label: String) {
  SHORT_EN("11/25 WED"),
  JP_DAY("11月25日(水)"),
  ISO_DATE("2026.11.25"),
  HIDDEN("日付非表示"),
}

enum class ClockFontWeightLevel(val label: String) {
  LIGHT("細字"),
  MEDIUM("標準"),
  BOLD("太字"),
}

data class ClockStyleConfig(
  val styleId: ClockStyleId = ClockStyleId.ORBIT_DIGITAL,
  val accentColorHex: Long = 0xFF3EF0D0,
  val secondaryColorHex: Long = 0xFFFFB84D,
  val hourTextColorHex: Long = 0xFFFFFFFF,
  val showSeconds: Boolean = true,
  val smoothSweepSeconds: Boolean = true,
  val use24Hour: Boolean = true,
  val clockScale: Float = 1.0f,
  val clockOpacity: Float = 1.0f,
  val offsetXFraction: Float = 0f,
  val offsetYFraction: Float = 0f,
  val fontWeight: ClockFontWeightLevel = ClockFontWeightLevel.BOLD,
  val dateFormat: DateDisplayFormat = DateDisplayFormat.SHORT_EN,
  val showModelTag: Boolean = false,
  val ambientMonochrome: Boolean = true,
  val tickStyle: TickStyle = TickStyle.PRECISION_60,
)

enum class WallpaperSourceType(val label: String) {
  PRESET_ART("プリセットアート"),
  PROCEDURAL_SHADER("シェーダーアニメーション"),
  LOCAL_VIDEO_URI("動画壁紙 (MP4/MOV)"),
}

enum class ShaderPresetId(val title: String, val description: String) {
  AURORA_WAVE("オーロラウェーブ", "深黒OLED背景上で揺らぐシアン・オーロラ"),
  ORBITAL_RADAR("オービタルレーダー", "回転スキャンと精密同心円パルス"),
  STARFIELD_DRIFT("スターフィールド", "微細パーティクルの低負荷ドリフト"),
  PURE_OLED_CARBON("ピュアOLEDブラック", "消費電力最小の純黒カーボン面"),
}

data class WallpaperConfig(
  val sourceType: WallpaperSourceType = WallpaperSourceType.PROCEDURAL_SHADER,
  val presetArtId: String = "preset_aurora",
  val shaderPresetId: ShaderPresetId = ShaderPresetId.AURORA_WAVE,
  val videoUriString: String = "",
  val videoTitle: String = "未選択",
  val loopEnabled: Boolean = true,
  val playbackSpeed: Float = 1.0f,
  val dimOpacity: Float = 0.35f,
  val targetFps: Int = 24,
  val downscalePx: Int = 456,
  val pauseInAmbient: Boolean = true,
)

enum class TimePeriodSlot(val label: String, val startHour: Int, val endHourExclusive: Int) {
  MORNING("朝 (06:00–11:59)", 6, 12),
  DAY("昼 (12:00–17:59)", 12, 18),
  EVENING("夜 (18:00–23:59)", 18, 24),
  LATE_NIGHT("深夜 (00:00–05:59)", 0, 6),
}

data class TimeWallpaperRule(
  val period: TimePeriodSlot,
  val enabled: Boolean = false,
  val sourceType: WallpaperSourceType = WallpaperSourceType.PROCEDURAL_SHADER,
  val presetArtId: String = "preset_aurora",
  val shaderPresetId: ShaderPresetId = ShaderPresetId.AURORA_WAVE,
  val videoUriString: String = "",
  val videoTitle: String = "未選択",
  val dimOpacity: Float = 0.35f,
)

data class WallpaperAutomationConfig(
  val switchOnOffWrist: Boolean = false,
  val offWristSourceType: WallpaperSourceType = WallpaperSourceType.PROCEDURAL_SHADER,
  val offWristShaderPreset: ShaderPresetId = ShaderPresetId.PURE_OLED_CARBON,
  val offWristPresetArtId: String = "preset_horology",
  val offWristVideoUriString: String = "",
  val offWristVideoTitle: String = "未選択",
  val offWristDimOpacity: Float = 0.65f,
  val timeScheduleEnabled: Boolean = false,
  val timeRules: List<TimeWallpaperRule> = defaultTimeRules(),
) {
  companion object {
    fun defaultTimeRules(): List<TimeWallpaperRule> = listOf(
      TimeWallpaperRule(
        period = TimePeriodSlot.MORNING,
        enabled = true,
        sourceType = WallpaperSourceType.PROCEDURAL_SHADER,
        shaderPresetId = ShaderPresetId.AURORA_WAVE,
        dimOpacity = 0.25f,
      ),
      TimeWallpaperRule(
        period = TimePeriodSlot.DAY,
        enabled = true,
        sourceType = WallpaperSourceType.PRESET_ART,
        presetArtId = "preset_aurora",
        dimOpacity = 0.30f,
      ),
      TimeWallpaperRule(
        period = TimePeriodSlot.EVENING,
        enabled = true,
        sourceType = WallpaperSourceType.PRESET_ART,
        presetArtId = "preset_horology",
        dimOpacity = 0.40f,
      ),
      TimeWallpaperRule(
        period = TimePeriodSlot.LATE_NIGHT,
        enabled = true,
        sourceType = WallpaperSourceType.PROCEDURAL_SHADER,
        shaderPresetId = ShaderPresetId.PURE_OLED_CARBON,
        dimOpacity = 0.65f,
      ),
    )
  }
}

enum class NotificationEdgeStyle(val label: String) {
  PULSE_RING("全周パルス発光"),
  ORBIT_SWEEP("回転スイープ光"),
  DOUBLE_FLASH("点滅アラート"),
}

data class NotificationAlertConfig(
  val edgeGlowEnabled: Boolean = true,
  val edgeGlowColorHex: Long = 0xFF3EF0D0,
  val edgeGlowStyle: NotificationEdgeStyle = NotificationEdgeStyle.PULSE_RING,
  val edgeGlowThicknessDp: Float = 4.5f,
  val glowWhileUnread: Boolean = true,
  val flashDurationSec: Int = 6,
)

enum class WidgetSlotPosition(
  val label: String,
  val shortLabel: String,
  val defaultX: Float,
  val defaultY: Float,
) {
  TOP_ARC("上部スロット", "上", 0f, -0.34f),
  LEFT_DIAL("左スロット", "左", -0.31f, -0.14f),
  RIGHT_DIAL("右スロット", "右", 0.31f, -0.14f),
  BOTTOM_DOCK("下部スロット", "下", 0f, 0.34f),
}

data class WidgetSlotConfig(
  val position: WidgetSlotPosition,
  val pluginId: String,
  val customTitle: String? = null,
  val customValue: String? = null,
  val showInAmbient: Boolean = true,
  val accentOverrideHex: Long? = null,
  val offsetXFraction: Float = position.defaultX,
  val offsetYFraction: Float = position.defaultY,
  val scale: Float = 1.0f,
  val backgroundAlpha: Float = 0.72f,
)

data class AppDockShortcut(
  val id: String,
  val label: String,
  val iconKey: String,
  val packageHint: String,
  val enabled: Boolean = true,
)

enum class BatteryProfile(val title: String, val description: String) {
  ULTRA_ENDURANCE("ウルトラ省電力 (48h+)", "AOD動画停止・15fps・強OLED黒マスク"),
  BALANCED("バランス最適化 (推奨)", "AOD時1Hz化・24fps動画・適度な減光"),
  MAX_VISUAL("ビジュアル優先", "30fps滑らか動画・高輝度・常時ウィジェット更新"),
  CUSTOM("カスタム設定", "詳細パラメータを個別に調整"),
}

data class BatteryOptimizationConfig(
  val profile: BatteryProfile = BatteryProfile.BALANCED,
  val pauseVideoInAod: Boolean = true,
  val ltpoAmbient1Hz: Boolean = true,
  val maxVideoFps: Int = 24,
  val oledBlackBoost: Float = 0.35f,
  val complicationRefreshSec: Int = 30,
  val syncOnlyOnCharger: Boolean = false,
  val autoSaverThresholdPct: Int = 20,
)

data class CustomWidgetExtension(
  val pluginId: String,
  val title: String,
  val shortTag: String,
  val defaultValue: String,
  val unit: String,
  val iconKey: String,
  val category: String = "カスタム拡張",
  val refreshIntervalSec: Int = 60,
  val supportsAmbient: Boolean = true,
)

data class WatchHomeConfig(
  val id: String = "active_config",
  val name: String = "Pixel Watch 3 Studio",
  val watchModel: WatchModelSpec = WatchModelSpec.PIXEL_WATCH_3_45MM,
  val clockStyle: ClockStyleConfig = ClockStyleConfig(),
  val wallpaper: WallpaperConfig = WallpaperConfig(),
  val wallpaperAutomation: WallpaperAutomationConfig = WallpaperAutomationConfig(),
  val notificationAlert: NotificationAlertConfig = NotificationAlertConfig(),
  val widgetSlots: List<WidgetSlotConfig> = defaultWidgetSlots(),
  val showAppDockButton: Boolean = true,
  val appDockShortcuts: List<AppDockShortcut> = defaultAppDockShortcuts(),
  val battery: BatteryOptimizationConfig = BatteryOptimizationConfig(),
  val customExtensions: List<CustomWidgetExtension> = emptyList(),
  val updatedAtEpochMs: Long = 0L,
) {
  companion object {
    fun defaultWidgetSlots(): List<WidgetSlotConfig> =
      listOf(
        WidgetSlotConfig(
          position = WidgetSlotPosition.TOP_ARC,
          pluginId = "sys_notifications",
          showInAmbient = true,
          offsetXFraction = 0f,
          offsetYFraction = -0.34f,
        ),
        WidgetSlotConfig(
          position = WidgetSlotPosition.LEFT_DIAL,
          pluginId = "health_hr",
          showInAmbient = false,
          offsetXFraction = -0.31f,
          offsetYFraction = -0.15f,
        ),
        WidgetSlotConfig(
          position = WidgetSlotPosition.RIGHT_DIAL,
          pluginId = "sys_battery",
          showInAmbient = true,
          offsetXFraction = 0.31f,
          offsetYFraction = -0.15f,
        ),
        WidgetSlotConfig(
          position = WidgetSlotPosition.BOTTOM_DOCK,
          pluginId = "health_steps",
          showInAmbient = true,
          offsetXFraction = 0f,
          offsetYFraction = 0.34f,
        ),
      )

    fun defaultAppDockShortcuts(): List<AppDockShortcut> =
      listOf(
        AppDockShortcut("workout", "ワークアウト", "fitness", "com.google.android.apps.fitness"),
        AppDockShortcut("media", "メディア", "music", "com.google.android.apps.wearable.media"),
        AppDockShortcut("wallet", "ウォレット", "wallet", "com.google.android.apps.walletnfcrel"),
        AppDockShortcut("maps", "マップ", "map", "com.google.android.apps.maps"),
        AppDockShortcut("timer", "タイマー", "timer", "com.google.android.deskclock"),
        AppDockShortcut("flashlight", "ライト", "flashlight", "system.flashlight"),
      )
  }
}

data class WatchTelemetrySnapshot(
  val watchBatteryPct: Int = 84,
  val phoneBatteryPct: Int = 92,
  val heartRateBpm: Int = 68,
  val stepsCount: Int = 7420,
  val stepsGoal: Int = 10000,
  val temperatureC: Int = 21,
  val weatherLabel: String = "晴",
  val nextEventLabel: String = "15:00 定例",
  val mediaTrackTitle: String = "Night Drive",
  val unreadNotificationCount: Int = 0,
  val lastNotificationPulseMs: Long = 0L,
  val isOffWrist: Boolean = false,
)
