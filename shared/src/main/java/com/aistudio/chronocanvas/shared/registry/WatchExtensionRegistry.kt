package com.aistudio.chronocanvas.shared.registry

import com.aistudio.chronocanvas.shared.model.CustomWidgetExtension
import com.aistudio.chronocanvas.shared.model.WatchTelemetrySnapshot
import com.aistudio.chronocanvas.shared.model.WidgetSlotConfig

const val PLUGIN_NONE = "none"

interface WidgetPlugin {
  val id: String
  val displayName: String
  val category: String
  val description: String
  val iconKey: String
  val defaultRefreshSec: Int
  val supportsProgressRing: Boolean
  val isBuiltIn: Boolean

  fun resolveDisplay(
    slot: WidgetSlotConfig,
    telemetry: WatchTelemetrySnapshot,
  ): WidgetResolvedDisplay
}

data class WidgetResolvedDisplay(
  val title: String,
  val primaryValue: String,
  val progress: Float? = null,
  val iconKey: String,
)

class BuiltInWidgetPlugin(
  override val id: String,
  override val displayName: String,
  override val category: String,
  override val description: String,
  override val iconKey: String,
  override val defaultRefreshSec: Int,
  override val supportsProgressRing: Boolean,
  private val resolver: (WidgetSlotConfig, WatchTelemetrySnapshot) -> WidgetResolvedDisplay,
) : WidgetPlugin {
  override val isBuiltIn: Boolean = true

  override fun resolveDisplay(
    slot: WidgetSlotConfig,
    telemetry: WatchTelemetrySnapshot,
  ): WidgetResolvedDisplay = resolver(slot, telemetry)
}

class CustomTemplateWidgetPlugin(
  private val spec: CustomWidgetExtension,
) : WidgetPlugin {
  override val id: String = spec.pluginId
  override val displayName: String = spec.title
  override val category: String = spec.category
  override val description: String = "ユーザー定義カスタム拡張 (${spec.shortTag})"
  override val iconKey: String = spec.iconKey
  override val defaultRefreshSec: Int = spec.refreshIntervalSec
  override val supportsProgressRing: Boolean = false
  override val isBuiltIn: Boolean = false

  override fun resolveDisplay(
    slot: WidgetSlotConfig,
    telemetry: WatchTelemetrySnapshot,
  ): WidgetResolvedDisplay {
    val label = slot.customTitle?.takeIf { it.isNotBlank() } ?: spec.shortTag
    val value = slot.customValue?.takeIf { it.isNotBlank() }
      ?: "${spec.defaultValue}${spec.unit}"
    return WidgetResolvedDisplay(
      title = label,
      primaryValue = value,
      progress = null,
      iconKey = spec.iconKey,
    )
  }
}

object WatchExtensionRegistry {

  private val builtInPlugins: List<WidgetPlugin> = listOf(
    BuiltInWidgetPlugin(
      id = PLUGIN_NONE,
      displayName = "なし（非表示）",
      category = "基本",
      description = "この枠にウィジェットを表示しません（時計や壁紙を広く見せます）",
      iconKey = "none",
      defaultRefreshSec = 3600,
      supportsProgressRing = false,
    ) { _, _ ->
      WidgetResolvedDisplay(
        title = "",
        primaryValue = "",
        progress = null,
        iconKey = "none",
      )
    },
    BuiltInWidgetPlugin(
      id = "sys_notifications",
      displayName = "未読通知件数",
      category = "システム",
      description = "現在届いている未読通知の件数をリアルタイム表示",
      iconKey = "notifications",
      defaultRefreshSec = 5,
      supportsProgressRing = true,
    ) { slot, t ->
      val count = t.unreadNotificationCount
      WidgetResolvedDisplay(
        title = slot.customTitle?.takeIf { it.isNotBlank() } ?: "通知",
        primaryValue = if (count > 0) "${count}件" else "0件",
        progress = (count / 10f).coerceIn(0f, 1f),
        iconKey = "notifications",
      )
    },
    BuiltInWidgetPlugin(
      id = "sys_battery",
      displayName = "バッテリー残量",
      category = "システム",
      description = "Pixel Watch 3のバッテリー残量とリングゲージを表示",
      iconKey = "battery",
      defaultRefreshSec = 60,
      supportsProgressRing = true,
    ) { slot, t ->
      WidgetResolvedDisplay(
        title = slot.customTitle?.takeIf { it.isNotBlank() } ?: "BAT",
        primaryValue = "${t.watchBatteryPct}%",
        progress = (t.watchBatteryPct / 100f).coerceIn(0f, 1f),
        iconKey = "battery",
      )
    },
    BuiltInWidgetPlugin(
      id = "health_steps",
      displayName = "歩数計",
      category = "ヘルス",
      description = "1日の目標歩数に対する達成リングと現在歩数",
      iconKey = "steps",
      defaultRefreshSec = 30,
      supportsProgressRing = true,
    ) { slot, t ->
      WidgetResolvedDisplay(
        title = slot.customTitle?.takeIf { it.isNotBlank() } ?: "STEPS",
        primaryValue = "${t.stepsCount}",
        progress = (t.stepsCount.toFloat() / t.stepsGoal.coerceAtLeast(1)).coerceIn(0f, 1f),
        iconKey = "steps",
      )
    },
    BuiltInWidgetPlugin(
      id = "health_hr",
      displayName = "心拍数 (BPM)",
      category = "ヘルス",
      description = "リアルタイム心拍数モニター",
      iconKey = "heart",
      defaultRefreshSec = 15,
      supportsProgressRing = true,
    ) { slot, t ->
      WidgetResolvedDisplay(
        title = slot.customTitle?.takeIf { it.isNotBlank() } ?: "BPM",
        primaryValue = "${t.heartRateBpm}",
        progress = ((t.heartRateBpm - 40f) / 140f).coerceIn(0.1f, 1f),
        iconKey = "heart",
      )
    },
    BuiltInWidgetPlugin(
      id = "sys_weather",
      displayName = "天気 & 気温",
      category = "環境",
      description = "現在地の天気アイコンと気温",
      iconKey = "weather",
      defaultRefreshSec = 900,
      supportsProgressRing = false,
    ) { slot, t ->
      WidgetResolvedDisplay(
        title = slot.customTitle?.takeIf { it.isNotBlank() } ?: t.weatherLabel,
        primaryValue = "${t.temperatureC}°C",
        progress = null,
        iconKey = "weather",
      )
    },
    BuiltInWidgetPlugin(
      id = "cal_next_event",
      displayName = "次の予定",
      category = "スケジュール",
      description = "カレンダーの直近スケジュールを表示",
      iconKey = "calendar",
      defaultRefreshSec = 300,
      supportsProgressRing = false,
    ) { slot, t ->
      WidgetResolvedDisplay(
        title = slot.customTitle?.takeIf { it.isNotBlank() } ?: "NEXT",
        primaryValue = slot.customValue?.takeIf { it.isNotBlank() } ?: t.nextEventLabel,
        progress = null,
        iconKey = "calendar",
      )
    },
    BuiltInWidgetPlugin(
      id = "media_now_playing",
      displayName = "メディア再生中",
      category = "メディア",
      description = "スマホで再生中のトラック名を表示・操作",
      iconKey = "music",
      defaultRefreshSec = 15,
      supportsProgressRing = false,
    ) { slot, t ->
      WidgetResolvedDisplay(
        title = slot.customTitle?.takeIf { it.isNotBlank() } ?: "MEDIA",
        primaryValue = slot.customValue?.takeIf { it.isNotBlank() } ?: t.mediaTrackTitle,
        progress = null,
        iconKey = "music",
      )
    },
    BuiltInWidgetPlugin(
      id = "dual_phone_bat",
      displayName = "スマホ電池連動",
      category = "システム",
      description = "ペアリング中のAndroidスマホ残量を表示",
      iconKey = "phone",
      defaultRefreshSec = 120,
      supportsProgressRing = true,
    ) { slot, t ->
      WidgetResolvedDisplay(
        title = slot.customTitle?.takeIf { it.isNotBlank() } ?: "PHONE",
        primaryValue = "${t.phoneBatteryPct}%",
        progress = (t.phoneBatteryPct / 100f).coerceIn(0f, 1f),
        iconKey = "phone",
      )
    },
    BuiltInWidgetPlugin(
      id = "custom_memo",
      displayName = "カスタムメモ / 目標",
      category = "ユーティリティ",
      description = "自由入力テキストをウォッチ画面に常駐表示",
      iconKey = "spark",
      defaultRefreshSec = 3600,
      supportsProgressRing = false,
    ) { slot, _ ->
      WidgetResolvedDisplay(
        title = slot.customTitle?.takeIf { it.isNotBlank() } ?: "NOTE",
        primaryValue = slot.customValue?.takeIf { it.isNotBlank() } ?: "Focus",
        progress = null,
        iconKey = "spark",
      )
    },
  )

  fun getAllPlugins(customExtensions: List<CustomWidgetExtension>): List<WidgetPlugin> {
    val custom = customExtensions.map { CustomTemplateWidgetPlugin(it) }
    return builtInPlugins + custom
  }

  fun findPlugin(
    pluginId: String,
    customExtensions: List<CustomWidgetExtension> = emptyList(),
  ): WidgetPlugin {
    return getAllPlugins(customExtensions).firstOrNull { it.id == pluginId }
      ?: builtInPlugins.first()
  }
}
