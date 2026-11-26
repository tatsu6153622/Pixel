package com.example.data

import android.content.Context
import androidx.room.Room
import com.aistudio.chronocanvas.shared.battery.BatteryOptimizerEngine
import com.aistudio.chronocanvas.shared.codec.WatchConfigCodec
import com.aistudio.chronocanvas.shared.model.BatteryProfile
import com.aistudio.chronocanvas.shared.model.ClockStyleId
import com.aistudio.chronocanvas.shared.model.ShaderPresetId
import com.aistudio.chronocanvas.shared.model.TickStyle
import com.aistudio.chronocanvas.shared.model.WallpaperConfig
import com.aistudio.chronocanvas.shared.model.WallpaperSourceType
import com.aistudio.chronocanvas.shared.model.WatchHomeConfig
import com.aistudio.chronocanvas.shared.model.WatchModelSpec
import com.aistudio.chronocanvas.shared.model.WidgetSlotConfig
import com.aistudio.chronocanvas.shared.model.WidgetSlotPosition
import com.example.data.local.ChronoDatabase
import com.example.data.local.StudioPreferences
import com.example.data.local.SyncLogEntity
import com.example.data.local.WatchPresetEntity
import com.example.notifications.PhoneTelemetryHelper
import com.example.wearsync.WatchAppStatus
import com.example.wearsync.WearNodeInfo
import com.example.wearsync.WearSyncManager
import com.example.wearsync.WearSyncResult
import kotlinx.coroutines.flow.Flow

class StudioRepository(private val context: Context) {

  private val db = Room.databaseBuilder(
    context.applicationContext,
    ChronoDatabase::class.java,
    "chronocanvas.db",
  ).build()

  private val dao = db.chronoDao()
  private val prefs = StudioPreferences(context.applicationContext)
  private val wearSync = WearSyncManager(context.applicationContext)

  val activeConfigFlow: Flow<WatchHomeConfig> = prefs.activeConfigFlow
  val autoSyncEnabledFlow: Flow<Boolean> = prefs.autoSyncEnabledFlow
  val presetsFlow: Flow<List<WatchPresetEntity>> = dao.observePresets()
  val syncLogsFlow: Flow<List<SyncLogEntity>> = dao.observeSyncLogs()

  suspend fun seedBuiltInPresetsIfNeeded() {
    val builtIns = listOf(
      WatchPresetEntity(
        id = "builtin_aurora_orbit",
        name = "オーロラ・オービット",
        summary = "太字デジタル時計 + シアンオーロラ + バランス省電力",
        configJson = WatchConfigCodec.toJson(
          WatchHomeConfig(
            id = "builtin_aurora_orbit",
            name = "オーロラ・オービット",
            wallpaper = WallpaperConfig(
              sourceType = WallpaperSourceType.PRESET_ART,
              presetArtId = "preset_aurora",
              dimOpacity = 0.30f,
            ),
          ),
        ),
        isBuiltIn = true,
      ),
      WatchPresetEntity(
        id = "builtin_horology_chrono",
        name = "ラグジュアリー・クロノ",
        summary = "精密アナログ針 + カーボンゴールド背景 + 60分目盛り",
        configJson = WatchConfigCodec.toJson(
          WatchHomeConfig(
            id = "builtin_horology_chrono",
            name = "ラグジュアリー・クロノ",
            clockStyle = com.aistudio.chronocanvas.shared.model.ClockStyleConfig(
              styleId = ClockStyleId.CHRONO_ANALOG,
              accentColorHex = 0xFFFFB84D,
              secondaryColorHex = 0xFF3EF0D0,
              tickStyle = TickStyle.PRECISION_60,
            ),
            wallpaper = WallpaperConfig(
              sourceType = WallpaperSourceType.PRESET_ART,
              presetArtId = "preset_horology",
              dimOpacity = 0.25f,
            ),
          ),
        ),
        isBuiltIn = true,
      ),
      WatchPresetEntity(
        id = "builtin_ultra_oled",
        name = "48h+ ウルトラOLED",
        summary = "ミニマル時計 + 純黒OLED + LTPO 1Hz最大省電力",
        configJson = WatchConfigCodec.toJson(
          BatteryOptimizerEngine.applyProfile(
            WatchHomeConfig(
              id = "builtin_ultra_oled",
              name = "48h+ ウルトラOLED",
              clockStyle = com.aistudio.chronocanvas.shared.model.ClockStyleConfig(
                styleId = ClockStyleId.MODULAR_MINIMAL,
                accentColorHex = 0xFF5CF28E,
                showSeconds = false,
              ),
              wallpaper = WallpaperConfig(
                sourceType = WallpaperSourceType.PROCEDURAL_SHADER,
                shaderPresetId = ShaderPresetId.PURE_OLED_CARBON,
                dimOpacity = 0.65f,
              ),
              widgetSlots = listOf(
                WidgetSlotConfig(WidgetSlotPosition.TOP_ARC, "sys_battery", showInAmbient = true),
                WidgetSlotConfig(WidgetSlotPosition.LEFT_DIAL, "health_steps", showInAmbient = true),
                WidgetSlotConfig(WidgetSlotPosition.RIGHT_DIAL, "sys_weather", showInAmbient = false),
                WidgetSlotConfig(WidgetSlotPosition.BOTTOM_DOCK, "cal_next_event", showInAmbient = true),
              ),
            ),
            BatteryProfile.ULTRA_ENDURANCE,
          ),
        ),
        isBuiltIn = true,
      ),
      WatchPresetEntity(
        id = "builtin_telemetry_matrix",
        name = "テレメトリ・マトリクス",
        summary = "技術計器レイアウト + レーダーシェーダー + 心拍/電池連動",
        configJson = WatchConfigCodec.toJson(
          WatchHomeConfig(
            id = "builtin_telemetry_matrix",
            name = "テレメトリ・マトリクス",
            watchModel = WatchModelSpec.PIXEL_WATCH_3_45MM,
            clockStyle = com.aistudio.chronocanvas.shared.model.ClockStyleConfig(
              styleId = ClockStyleId.DATA_MATRIX,
              accentColorHex = 0xFF3EF0D0,
              secondaryColorHex = 0xFFFF6B7A,
            ),
            wallpaper = WallpaperConfig(
              sourceType = WallpaperSourceType.PROCEDURAL_SHADER,
              shaderPresetId = ShaderPresetId.ORBITAL_RADAR,
              dimOpacity = 0.35f,
            ),
            widgetSlots = listOf(
              WidgetSlotConfig(WidgetSlotPosition.TOP_ARC, "dual_phone_bat", showInAmbient = true),
              WidgetSlotConfig(WidgetSlotPosition.LEFT_DIAL, "health_hr", showInAmbient = false),
              WidgetSlotConfig(WidgetSlotPosition.RIGHT_DIAL, "sys_battery", showInAmbient = true),
              WidgetSlotConfig(WidgetSlotPosition.BOTTOM_DOCK, "media_now_playing", showInAmbient = false),
            ),
          ),
        ),
        isBuiltIn = true,
      ),
    )
    dao.insertPresetsIfMissing(builtIns)
  }

  suspend fun saveActiveConfig(config: WatchHomeConfig) {
    prefs.saveActiveConfig(config)
  }

  suspend fun saveCustomPreset(name: String, config: WatchHomeConfig) {
    val presetId = "custom_${System.currentTimeMillis()}"
    val stamped = config.copy(id = presetId, name = name, updatedAtEpochMs = System.currentTimeMillis())
    dao.upsertPreset(
      WatchPresetEntity(
        id = presetId,
        name = name,
        summary = "${stamped.clockStyle.styleId.title} · ${stamped.wallpaper.sourceType.label}",
        configJson = WatchConfigCodec.toJson(stamped),
        isBuiltIn = false,
      ),
    )
  }

  suspend fun deleteCustomPreset(id: String) {
    dao.deleteCustomPreset(id)
  }

  suspend fun setAutoSyncEnabled(enabled: Boolean) {
    prefs.setAutoSyncEnabled(enabled)
  }

  suspend fun scanWearNodes(): Pair<Boolean, List<WearNodeInfo>> = wearSync.scanConnectedNodes()

  suspend fun queryWatchAppStatus(nodes: List<WearNodeInfo>): WatchAppStatus? =
    wearSync.queryWatchAppStatus(nodes)

  suspend fun syncOtaIncremental(config: WatchHomeConfig): WearSyncResult =
    wearSync.syncConfigToWatch(config, forceResendVideos = false)

  suspend fun sendForceRefreshToWatch(): Result<String> = wearSync.sendForceRefreshToWatch()

  suspend fun sendLaunchHomeToWatch(): Result<String> = wearSync.sendLaunchHomeToWatch()

  suspend fun launchRemoteWatchUpdate(
    nodeId: String? = null,
    customReleaseUrl: String? = null,
  ): Result<String> = wearSync.launchRemoteWatchUpdate(nodeId, customReleaseUrl)

  suspend fun syncToPixelWatch(config: WatchHomeConfig): WearSyncResult {
    val result = wearSync.syncConfigToWatch(config, forceResendVideos = true)
    val logEntry = when (result) {
      is WearSyncResult.SyncedToWatch -> SyncLogEntity(
        targetNodeName = result.primaryNodeName,
        statusLabel = "同期完了",
        payloadBytes = result.payloadBytes,
        includedVideo = result.videoTransferred,
        detailMessage = result.message,
      )
      is WearSyncResult.NoPairedWatchFound -> SyncLogEntity(
        targetNodeName = "未接続（ローカル検証済み）",
        statusLabel = "ノード未検出",
        payloadBytes = result.payloadBytes,
        includedVideo = false,
        detailMessage = result.reason,
      )
      is WearSyncResult.Error -> SyncLogEntity(
        targetNodeName = "Wear OS Data Layer",
        statusLabel = "エラー",
        payloadBytes = 0,
        includedVideo = false,
        detailMessage = result.message,
      )
    }
    dao.insertSyncLog(logEntry)
    return result
  }

  suspend fun pushNotificationTelemetry(unreadCount: Int, pulseMs: Long) {
    wearSync.pushNotificationTelemetry(unreadCount, pulseMs)
  }

  suspend fun pushTelemetryToWatch(unreadCount: Int, pulseMs: Long, phoneBatteryPct: Int) {
    wearSync.pushTelemetryToWatch(unreadCount, pulseMs, phoneBatteryPct)
  }

  fun readPhoneBatteryPct(): Int = PhoneTelemetryHelper.readPhoneBatteryPct(context)
}
