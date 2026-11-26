package com.example.ui.studio

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.chronocanvas.shared.battery.BatteryOptimizerEngine
import com.aistudio.chronocanvas.shared.battery.BatteryPowerReport
import com.aistudio.chronocanvas.shared.codec.WatchConfigCodec
import com.aistudio.chronocanvas.shared.model.BatteryProfile
import com.aistudio.chronocanvas.shared.model.ClockFontWeightLevel
import com.aistudio.chronocanvas.shared.model.ClockStyleId
import com.aistudio.chronocanvas.shared.model.CustomWidgetExtension
import com.aistudio.chronocanvas.shared.model.DateDisplayFormat
import com.aistudio.chronocanvas.shared.model.NotificationEdgeStyle
import com.aistudio.chronocanvas.shared.model.ShaderPresetId
import com.aistudio.chronocanvas.shared.model.TickStyle
import com.aistudio.chronocanvas.shared.model.TimePeriodSlot
import com.aistudio.chronocanvas.shared.model.WallpaperSourceType
import com.aistudio.chronocanvas.shared.model.WatchHomeConfig
import com.aistudio.chronocanvas.shared.model.WatchModelSpec
import com.aistudio.chronocanvas.shared.model.WatchTelemetrySnapshot
import com.aistudio.chronocanvas.shared.model.WidgetSlotPosition
import com.aistudio.chronocanvas.shared.registry.PLUGIN_NONE
import com.example.data.StudioRepository
import com.example.data.local.SyncLogEntity
import com.example.data.local.WatchPresetEntity
import com.example.media.VideoExtensionTranscoder
import com.example.media.VideoFileDetails
import com.example.media.WatchVideoExtensionPackage
import com.example.media.inspectVideoUri
import com.example.notifications.ChronoNotificationListenerService
import com.example.notifications.NotificationBridgeState
import com.example.wearsync.WatchAppStatus
import com.example.wearsync.WatchLiveBridgeState
import com.example.wearsync.WearNodeInfo
import com.example.wearsync.WearSyncResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudioTab(val route: String, val label: String) {
  STUDIO("studio", "スタジオ"),
  CLOCK_WIDGETS("clock_widgets", "時計・拡張"),
  WALLPAPER("wallpaper", "動画壁紙"),
  BATTERY_SYNC("battery_sync", "省電力・同期"),
  GUIDE("guide", "ガイド"),
}

data class StudioUiState(
  val activeConfig: WatchHomeConfig = WatchHomeConfig(),
  val telemetry: WatchTelemetrySnapshot = WatchTelemetrySnapshot(),
  val powerReport: BatteryPowerReport = BatteryOptimizerEngine.analyze(WatchHomeConfig()),
  val isAmbientPreview: Boolean = false,
  val showAppDockPreview: Boolean = false,
  val isFullscreenWatchMode: Boolean = false,
  val previewHourOverride: Int? = null,
  val notificationAccessGranted: Boolean = false,
  val notificationListenerConnected: Boolean = false,
  val selectedSlotForEdit: WidgetSlotPosition = WidgetSlotPosition.TOP_ARC,
  val savedPresets: List<WatchPresetEntity> = emptyList(),
  val syncLogs: List<SyncLogEntity> = emptyList(),
  val connectedNodes: List<WearNodeInfo> = emptyList(),
  val wearApiAvailable: Boolean = false,
  val isScanningNodes: Boolean = false,
  val isSyncing: Boolean = false,
  val lastSyncResult: WearSyncResult? = null,
  val autoSyncEnabled: Boolean = true,
  val watchAppStatus: WatchAppStatus? = null,
  val lastOtaSyncMs: Long? = null,
  val isLaunchingRemoteUpdate: Boolean = false,
  val selectedVideoDetails: VideoFileDetails? = null,
  val videoExtensionPackage: WatchVideoExtensionPackage? = null,
  val isProcessingVideoExtension: Boolean = false,
  val snackbarMessage: String? = null,
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = StudioRepository(application)
  private var debouncedOtaSyncJob: Job? = null

  private val _uiState = MutableStateFlow(
    StudioUiState(
      telemetry = WatchTelemetrySnapshot(
        phoneBatteryPct = repository.readPhoneBatteryPct(),
      ),
      notificationAccessGranted = NotificationBridgeState.isNotificationAccessEnabled(application),
    ),
  )
  val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

  private val batteryReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
      val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
      val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
      val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100)
      else repository.readPhoneBatteryPct()
      if (pct != _uiState.value.telemetry.phoneBatteryPct) {
        _uiState.update { state ->
          state.copy(telemetry = state.telemetry.copy(phoneBatteryPct = pct))
        }
        viewModelScope.launch {
          val t = _uiState.value.telemetry
          repository.pushTelemetryToWatch(t.unreadNotificationCount, t.lastNotificationPulseMs, pct)
        }
      }
    }
  }

  init {
    NotificationBridgeState.initialize(application)
    runCatching {
      ContextCompat.registerReceiver(
        application,
        batteryReceiver,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_NOT_EXPORTED,
      )
    }
    ChronoNotificationListenerService.requestRebindIfNeeded(application)
    viewModelScope.launch {
      repository.seedBuiltInPresetsIfNeeded()
    }
    viewModelScope.launch {
      repository.activeConfigFlow.collectLatest { loaded ->
        _uiState.update { state ->
          state.copy(
            activeConfig = loaded,
            powerReport = BatteryOptimizerEngine.analyze(loaded),
          )
        }
      }
    }
    viewModelScope.launch {
      repository.presetsFlow.collectLatest { presets ->
        _uiState.update { it.copy(savedPresets = presets) }
      }
    }
    viewModelScope.launch {
      repository.syncLogsFlow.collectLatest { logs ->
        _uiState.update { it.copy(syncLogs = logs) }
      }
    }
    viewModelScope.launch {
      NotificationBridgeState.isListenerConnected.collectLatest { connected ->
        _uiState.update { it.copy(notificationListenerConnected = connected) }
      }
    }
    viewModelScope.launch {
      combine(
        NotificationBridgeState.unreadCount,
        NotificationBridgeState.lastPulseMs,
      ) { count, pulseMs -> count to pulseMs }.collectLatest { (count, pulseMs) ->
        val batPct = repository.readPhoneBatteryPct()
        _uiState.update { state ->
          state.copy(
            telemetry = state.telemetry.copy(
              phoneBatteryPct = batPct,
              unreadNotificationCount = count,
              lastNotificationPulseMs = pulseMs,
            ),
          )
        }
        repository.pushTelemetryToWatch(count, pulseMs, batPct)
      }
    }
    viewModelScope.launch {
      repository.autoSyncEnabledFlow.collectLatest { enabled ->
        _uiState.update { it.copy(autoSyncEnabled = enabled) }
      }
    }
    viewModelScope.launch {
      WatchLiveBridgeState.watchStatus.collectLatest { status ->
        _uiState.update { it.copy(watchAppStatus = status) }
      }
    }
    viewModelScope.launch {
      WatchLiveBridgeState.autoSyncTrigger.collectLatest { trigger ->
        if (trigger > 0) {
          refreshWearNodes()
          if (_uiState.value.autoSyncEnabled) {
            val result = repository.syncOtaIncremental(_uiState.value.activeConfig)
            if (result is WearSyncResult.SyncedToWatch) {
              _uiState.update { it.copy(lastOtaSyncMs = System.currentTimeMillis(), lastSyncResult = result) }
            }
          }
        }
      }
    }
    refreshWearNodes()
    syncLiveTelemetryNow()
  }

  override fun onCleared() {
    runCatching { getApplication<Application>().unregisterReceiver(batteryReceiver) }
    super.onCleared()
  }

  private fun updateConfig(transform: (WatchHomeConfig) -> WatchHomeConfig) {
    val updated = transform(_uiState.value.activeConfig).copy(
      updatedAtEpochMs = System.currentTimeMillis(),
    )
    _uiState.update {
      it.copy(
        activeConfig = updated,
        powerReport = BatteryOptimizerEngine.analyze(updated),
      )
    }
    viewModelScope.launch {
      repository.saveActiveConfig(updated)
    }
    scheduleOtaSyncIfEnabled(updated)
  }

  private fun scheduleOtaSyncIfEnabled(config: WatchHomeConfig) {
    if (!_uiState.value.autoSyncEnabled || _uiState.value.connectedNodes.isEmpty()) return
    debouncedOtaSyncJob?.cancel()
    debouncedOtaSyncJob = viewModelScope.launch {
      delay(750L)
      val result = repository.syncOtaIncremental(config)
      if (result is WearSyncResult.SyncedToWatch) {
        _uiState.update {
          it.copy(
            lastOtaSyncMs = System.currentTimeMillis(),
            lastSyncResult = result,
          )
        }
      }
    }
  }

  fun refreshNotificationPermissionStatus() {
    val app = getApplication<Application>()
    val granted = NotificationBridgeState.isNotificationAccessEnabled(app)
    _uiState.update { it.copy(notificationAccessGranted = granted) }
    if (granted) {
      ChronoNotificationListenerService.requestRebindIfNeeded(app)
    }
  }

  fun syncLiveTelemetryNow() {
    val app = getApplication<Application>()
    val batPct = repository.readPhoneBatteryPct()
    _uiState.update { state ->
      state.copy(telemetry = state.telemetry.copy(phoneBatteryPct = batPct))
    }
    viewModelScope.launch {
      ChronoNotificationListenerService.syncCurrentActiveNotificationsToWatch(app)
    }
  }

  fun setWatchModel(model: WatchModelSpec) {
    updateConfig { cfg ->
      cfg.copy(
        watchModel = model,
        wallpaper = cfg.wallpaper.copy(downscalePx = model.nativePx),
      )
    }
  }

  fun toggleAmbientPreview(ambient: Boolean) {
    _uiState.update {
      it.copy(
        isAmbientPreview = ambient,
        showAppDockPreview = if (ambient) false else it.showAppDockPreview,
      )
    }
  }

  fun toggleAppDockPreview(show: Boolean) {
    _uiState.update { it.copy(showAppDockPreview = show) }
  }

  fun toggleSimOffWrist(offWrist: Boolean) {
    _uiState.update {
      it.copy(telemetry = it.telemetry.copy(isOffWrist = offWrist))
    }
  }

  fun setPreviewHourOverride(hour: Int?) {
    _uiState.update { it.copy(previewHourOverride = hour) }
  }

  fun triggerTestNotificationPulse() {
    val app = getApplication<Application>()
    val nextCount = (_uiState.value.telemetry.unreadNotificationCount + 1).coerceAtMost(99)
    val now = NotificationBridgeState.updateFromSystem(nextCount, triggerPulse = true, context = app)
    val batPct = repository.readPhoneBatteryPct()
    _uiState.update {
      it.copy(
        telemetry = it.telemetry.copy(
          phoneBatteryPct = batPct,
          unreadNotificationCount = nextCount,
          lastNotificationPulseMs = now,
        ),
        snackbarMessage = "通知テスト発火（未読${nextCount}件・フレーム発光）",
      )
    }
    viewModelScope.launch {
      repository.pushTelemetryToWatch(nextCount, now, batPct)
    }
  }

  fun clearNotificationsPreview() {
    val app = getApplication<Application>()
    NotificationBridgeState.updateFromSystem(0, triggerPulse = false, context = app)
    val batPct = repository.readPhoneBatteryPct()
    _uiState.update {
      it.copy(
        telemetry = it.telemetry.copy(
          phoneBatteryPct = batPct,
          unreadNotificationCount = 0,
          lastNotificationPulseMs = 0L,
        ),
      )
    }
    viewModelScope.launch {
      repository.pushTelemetryToWatch(0, 0L, batPct)
    }
  }

  fun setFullscreenWatchMode(fullscreen: Boolean) {
    _uiState.update { it.copy(isFullscreenWatchMode = fullscreen) }
  }

  fun selectSlotForEdit(position: WidgetSlotPosition) {
    _uiState.update { it.copy(selectedSlotForEdit = position) }
  }

  // Clock fine customization
  fun setClockStyle(styleId: ClockStyleId) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(styleId = styleId)) }
  }

  fun setClockAccent(hex: Long) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(accentColorHex = hex)) }
  }

  fun setClockHourColor(hex: Long) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(hourTextColorHex = hex)) }
  }

  fun setClockDateColor(hex: Long) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(dateTextColorHex = hex)) }
  }

  fun setClockScale(scale: Float) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(clockScale = scale.coerceIn(0.65f, 1.35f))) }
  }

  fun setClockOpacity(opacity: Float) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(clockOpacity = opacity.coerceIn(0.25f, 1f))) }
  }

  fun setClockOffset(offsetX: Float? = null, offsetY: Float? = null) {
    updateConfig { cfg ->
      cfg.copy(
        clockStyle = cfg.clockStyle.copy(
          offsetXFraction = (offsetX ?: cfg.clockStyle.offsetXFraction).coerceIn(-0.30f, 0.30f),
          offsetYFraction = (offsetY ?: cfg.clockStyle.offsetYFraction).coerceIn(-0.32f, 0.32f),
        ),
      )
    }
  }

  fun resetClockPosition() {
    updateConfig { cfg ->
      cfg.copy(clockStyle = cfg.clockStyle.copy(offsetXFraction = 0f, offsetYFraction = 0f, clockScale = 1.0f))
    }
  }

  fun setClockFontWeight(weight: ClockFontWeightLevel) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(fontWeight = weight)) }
  }

  fun setDateFormat(format: DateDisplayFormat) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(dateFormat = format)) }
  }

  fun setShowSeconds(show: Boolean) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(showSeconds = show)) }
  }

  fun setSmoothSweep(smooth: Boolean) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(smoothSweepSeconds = smooth)) }
  }

  fun setUse24Hour(use24: Boolean) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(use24Hour = use24)) }
  }

  fun setAmbientMonochrome(mono: Boolean) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(ambientMonochrome = mono)) }
  }

  fun setTickStyle(tickStyle: TickStyle) {
    updateConfig { it.copy(clockStyle = it.clockStyle.copy(tickStyle = tickStyle)) }
  }

  // Notification Edge Glow customization
  fun updateNotificationAlert(
    enabled: Boolean? = null,
    colorHex: Long? = null,
    style: NotificationEdgeStyle? = null,
    thicknessDp: Float? = null,
    glowWhileUnread: Boolean? = null,
    flashDurationSec: Int? = null,
  ) {
    updateConfig { cfg ->
      val na = cfg.notificationAlert
      cfg.copy(
        notificationAlert = na.copy(
          edgeGlowEnabled = enabled ?: na.edgeGlowEnabled,
          edgeGlowColorHex = colorHex ?: na.edgeGlowColorHex,
          edgeGlowStyle = style ?: na.edgeGlowStyle,
          edgeGlowThicknessDp = thicknessDp ?: na.edgeGlowThicknessDp,
          glowWhileUnread = glowWhileUnread ?: na.glowWhileUnread,
          flashDurationSec = flashDurationSec ?: na.flashDurationSec,
        ),
      )
    }
  }

  // Widget & Extension customization
  fun setShowAppDockButton(show: Boolean) {
    updateConfig { it.copy(showAppDockButton = show) }
  }

  fun assignPluginToSlot(position: WidgetSlotPosition, pluginId: String) {
    updateConfig { cfg ->
      val updatedSlots = cfg.widgetSlots.map { slot ->
        if (slot.position == position) slot.copy(pluginId = pluginId) else slot
      }
      cfg.copy(widgetSlots = updatedSlots)
    }
  }

  fun clearAllWidgets() {
    updateConfig { cfg ->
      cfg.copy(
        widgetSlots = cfg.widgetSlots.map { it.copy(pluginId = PLUGIN_NONE) },
      )
    }
    showMessage("すべてのウィジェットを非表示にしました")
  }

  fun updateSlotCustomization(
    position: WidgetSlotPosition,
    customTitle: String?,
    customValue: String?,
    showInAmbient: Boolean,
  ) {
    updateConfig { cfg ->
      val updatedSlots = cfg.widgetSlots.map { slot ->
        if (slot.position == position) {
          slot.copy(
            customTitle = customTitle?.takeIf { it.isNotBlank() },
            customValue = customValue?.takeIf { it.isNotBlank() },
            showInAmbient = showInAmbient,
          )
        } else {
          slot
        }
      }
      cfg.copy(widgetSlots = updatedSlots)
    }
  }

  fun updateSlotLayout(
    position: WidgetSlotPosition,
    offsetX: Float? = null,
    offsetY: Float? = null,
    scale: Float? = null,
    backgroundAlpha: Float? = null,
  ) {
    updateConfig { cfg ->
      val updatedSlots = cfg.widgetSlots.map { slot ->
        if (slot.position == position) {
          slot.copy(
            offsetXFraction = (offsetX ?: slot.offsetXFraction).coerceIn(-0.38f, 0.38f),
            offsetYFraction = (offsetY ?: slot.offsetYFraction).coerceIn(-0.40f, 0.40f),
            scale = (scale ?: slot.scale).coerceIn(0.65f, 1.35f),
            backgroundAlpha = (backgroundAlpha ?: slot.backgroundAlpha).coerceIn(0f, 0.95f),
          )
        } else {
          slot
        }
      }
      cfg.copy(widgetSlots = updatedSlots)
    }
  }

  fun resetAllPositionsToNonOverlappingDefaults() {
    updateConfig { cfg ->
      cfg.copy(
        clockStyle = cfg.clockStyle.copy(offsetXFraction = 0f, offsetYFraction = 0f, clockScale = 1.0f),
        widgetSlots = cfg.widgetSlots.map { slot ->
          slot.copy(
            offsetXFraction = slot.position.defaultX,
            offsetYFraction = slot.position.defaultY,
            scale = 1.0f,
          )
        },
      )
    }
    showMessage("時計とウィジェットを重なり防止レイアウトへリセットしました")
  }

  fun toggleAppDockShortcut(shortcutId: String, enabled: Boolean) {
    updateConfig { cfg ->
      cfg.copy(
        appDockShortcuts = cfg.appDockShortcuts.map {
          if (it.id == shortcutId) it.copy(enabled = enabled) else it
        },
      )
    }
  }

  fun registerCustomWidgetExtension(
    title: String,
    shortTag: String,
    defaultValue: String,
    unit: String,
    iconKey: String,
    assignToSelectedSlot: Boolean = true,
  ) {
    val cleanId = "ext_${System.currentTimeMillis()}"
    val ext = CustomWidgetExtension(
      pluginId = cleanId,
      title = title.trim().ifBlank { "カスタム拡張" },
      shortTag = shortTag.trim().ifBlank { "EXT" }.uppercase(),
      defaultValue = defaultValue.trim().ifBlank { "100" },
      unit = unit.trim(),
      iconKey = iconKey,
    )
    val slotPos = _uiState.value.selectedSlotForEdit
    updateConfig { cfg ->
      val newSlots = if (assignToSelectedSlot) {
        cfg.widgetSlots.map { s ->
          if (s.position == slotPos) s.copy(pluginId = cleanId, customTitle = null, customValue = null)
          else s
        }
      } else {
        cfg.widgetSlots
      }
      cfg.copy(
        customExtensions = cfg.customExtensions + ext,
        widgetSlots = newSlots,
      )
    }
    showMessage("カスタム拡張「${ext.title}」をプラグイン登録しました")
  }

  // Wallpaper & Video Extension
  fun setWallpaperSourceType(sourceType: WallpaperSourceType) {
    updateConfig { it.copy(wallpaper = it.wallpaper.copy(sourceType = sourceType)) }
  }

  fun setShaderPreset(shader: ShaderPresetId) {
    updateConfig {
      it.copy(
        wallpaper = it.wallpaper.copy(
          sourceType = WallpaperSourceType.PROCEDURAL_SHADER,
          shaderPresetId = shader,
        ),
      )
    }
  }

  fun setPresetArt(presetArtId: String) {
    updateConfig {
      it.copy(
        wallpaper = it.wallpaper.copy(
          sourceType = WallpaperSourceType.PRESET_ART,
          presetArtId = presetArtId,
        ),
      )
    }
  }

  fun onVideoPickedFromPhotoPicker(uri: Uri) {
    val context = getApplication<Application>()
    _uiState.update { it.copy(isProcessingVideoExtension = true) }
    viewModelScope.launch {
      val details = inspectVideoUri(context, uri)
      val cachedFile = VideoExtensionTranscoder.cachePickedVideoToPrivateStorage(context, uri, "active_video.mp4")
      val effectiveUri = cachedFile?.let { Uri.fromFile(it).toString() } ?: uri.toString()

      val cfg = _uiState.value.activeConfig
      val framePkg = VideoExtensionTranscoder.buildWatchFrameExtensionPreview(
        context = context,
        videoUri = Uri.parse(effectiveUri),
        targetResolutionPx = cfg.wallpaper.downscalePx,
        targetFps = cfg.wallpaper.targetFps,
      )

      updateConfig { current ->
        current.copy(
          wallpaper = current.wallpaper.copy(
            sourceType = WallpaperSourceType.LOCAL_VIDEO_URI,
            videoUriString = effectiveUri,
            videoTitle = details.title,
          ),
        )
      }
      _uiState.update {
        it.copy(
          selectedVideoDetails = details,
          videoExtensionPackage = framePkg,
          isProcessingVideoExtension = false,
          snackbarMessage = "動画壁紙「${details.title}」を拡張エンジンへ読み込みました",
        )
      }
    }
  }

  fun onOffWristVideoPicked(uri: Uri) {
    val context = getApplication<Application>()
    viewModelScope.launch {
      val details = inspectVideoUri(context, uri)
      val cachedFile = VideoExtensionTranscoder.cachePickedVideoToPrivateStorage(context, uri, "offwrist_video.mp4")
      val effectiveUri = cachedFile?.let { Uri.fromFile(it).toString() } ?: uri.toString()
      updateConfig { current ->
        current.copy(
          wallpaperAutomation = current.wallpaperAutomation.copy(
            switchOnOffWrist = true,
            offWristSourceType = WallpaperSourceType.LOCAL_VIDEO_URI,
            offWristVideoUriString = effectiveUri,
            offWristVideoTitle = details.title,
          ),
        )
      }
      showMessage("外した時用の動画「${details.title}」を設定しました")
    }
  }

  fun updateOffWristAutomation(
    enabled: Boolean? = null,
    sourceType: WallpaperSourceType? = null,
    shaderPreset: ShaderPresetId? = null,
    presetArtId: String? = null,
    dimOpacity: Float? = null,
  ) {
    updateConfig { cfg ->
      val auto = cfg.wallpaperAutomation
      cfg.copy(
        wallpaperAutomation = auto.copy(
          switchOnOffWrist = enabled ?: auto.switchOnOffWrist,
          offWristSourceType = sourceType ?: auto.offWristSourceType,
          offWristShaderPreset = shaderPreset ?: auto.offWristShaderPreset,
          offWristPresetArtId = presetArtId ?: auto.offWristPresetArtId,
          offWristDimOpacity = dimOpacity ?: auto.offWristDimOpacity,
        ),
      )
    }
  }

  fun toggleTimeSchedule(enabled: Boolean) {
    updateConfig { cfg ->
      cfg.copy(
        wallpaperAutomation = cfg.wallpaperAutomation.copy(timeScheduleEnabled = enabled),
      )
    }
  }

  fun updateTimeRule(
    period: TimePeriodSlot,
    enabled: Boolean? = null,
    sourceType: WallpaperSourceType? = null,
    shaderPresetId: ShaderPresetId? = null,
    presetArtId: String? = null,
    dimOpacity: Float? = null,
  ) {
    updateConfig { cfg ->
      val updatedRules = cfg.wallpaperAutomation.timeRules.map { rule ->
        if (rule.period == period) {
          rule.copy(
            enabled = enabled ?: rule.enabled,
            sourceType = sourceType ?: rule.sourceType,
            shaderPresetId = shaderPresetId ?: rule.shaderPresetId,
            presetArtId = presetArtId ?: rule.presetArtId,
            dimOpacity = dimOpacity ?: rule.dimOpacity,
          )
        } else {
          rule
        }
      }
      cfg.copy(
        wallpaperAutomation = cfg.wallpaperAutomation.copy(timeRules = updatedRules),
      )
    }
  }

  fun regenerateFrameExtensionPreview() {
    val uriStr = _uiState.value.activeConfig.wallpaper.videoUriString
    if (uriStr.isBlank()) return
    val context = getApplication<Application>()
    _uiState.update { it.copy(isProcessingVideoExtension = true) }
    viewModelScope.launch {
      val cfg = _uiState.value.activeConfig
      val framePkg = VideoExtensionTranscoder.buildWatchFrameExtensionPreview(
        context = context,
        videoUri = Uri.parse(uriStr),
        targetResolutionPx = cfg.wallpaper.downscalePx,
        targetFps = cfg.wallpaper.targetFps,
      )
      _uiState.update {
        it.copy(
          videoExtensionPackage = framePkg,
          isProcessingVideoExtension = false,
        )
      }
    }
  }

  fun updateWallpaperSettings(
    dimOpacity: Float? = null,
    targetFps: Int? = null,
    downscalePx: Int? = null,
    playbackSpeed: Float? = null,
    loopEnabled: Boolean? = null,
    pauseInAmbient: Boolean? = null,
  ) {
    updateConfig { cfg ->
      val wp = cfg.wallpaper
      cfg.copy(
        wallpaper = wp.copy(
          dimOpacity = dimOpacity ?: wp.dimOpacity,
          targetFps = targetFps ?: wp.targetFps,
          downscalePx = downscalePx ?: wp.downscalePx,
          playbackSpeed = playbackSpeed ?: wp.playbackSpeed,
          loopEnabled = loopEnabled ?: wp.loopEnabled,
          pauseInAmbient = pauseInAmbient ?: wp.pauseInAmbient,
        ),
        battery = cfg.battery.copy(
          profile = BatteryProfile.CUSTOM,
          maxVideoFps = targetFps ?: cfg.battery.maxVideoFps,
          oledBlackBoost = dimOpacity ?: cfg.battery.oledBlackBoost,
          pauseVideoInAod = pauseInAmbient ?: cfg.battery.pauseVideoInAod,
        ),
      )
    }
  }

  fun applyBatteryProfile(profile: BatteryProfile) {
    updateConfig { cfg -> BatteryOptimizerEngine.applyProfile(cfg, profile) }
    showMessage("${profile.title} を適用しました")
  }

  fun updateBatteryFlags(
    pauseVideoInAod: Boolean? = null,
    ltpoAmbient1Hz: Boolean? = null,
    complicationRefreshSec: Int? = null,
    syncOnlyOnCharger: Boolean? = null,
  ) {
    updateConfig { cfg ->
      val b = cfg.battery
      cfg.copy(
        wallpaper = cfg.wallpaper.copy(
          pauseInAmbient = pauseVideoInAod ?: cfg.wallpaper.pauseInAmbient,
        ),
        battery = b.copy(
          profile = BatteryProfile.CUSTOM,
          pauseVideoInAod = pauseVideoInAod ?: b.pauseVideoInAod,
          ltpoAmbient1Hz = ltpoAmbient1Hz ?: b.ltpoAmbient1Hz,
          complicationRefreshSec = complicationRefreshSec ?: b.complicationRefreshSec,
          syncOnlyOnCharger = syncOnlyOnCharger ?: b.syncOnlyOnCharger,
        ),
      )
    }
  }

  fun loadPreset(preset: WatchPresetEntity) {
    val parsed = runCatching { WatchConfigCodec.fromJson(preset.configJson) }.getOrNull() ?: return
    updateConfig { parsed }
    showMessage("プリセット「${preset.name}」を読み込みました")
  }

  fun saveCurrentAsPreset(name: String) {
    val clean = name.trim().ifBlank { "マイカスタム ${System.currentTimeMillis() % 1000}" }
    viewModelScope.launch {
      repository.saveCustomPreset(clean, _uiState.value.activeConfig)
      showMessage("「$clean」を保存しました")
    }
  }

  fun deleteCustomPreset(id: String) {
    viewModelScope.launch {
      repository.deleteCustomPreset(id)
    }
  }

  fun setAutoSyncEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setAutoSyncEnabled(enabled)
      if (enabled && _uiState.value.connectedNodes.isNotEmpty()) {
        val result = repository.syncOtaIncremental(_uiState.value.activeConfig)
        if (result is WearSyncResult.SyncedToWatch) {
          _uiState.update { it.copy(lastOtaSyncMs = System.currentTimeMillis(), lastSyncResult = result) }
        }
      }
    }
    showMessage(
      if (enabled) "スマホ連携OTA自動反映をONにしました" else "OTA自動反映をOFFにしました",
    )
  }

  fun updateLauncherSettings(
    keepAliveHomeMode: Boolean? = null,
    ambientAlwaysOnEnabled: Boolean? = null,
    autoLaunchOnBoot: Boolean? = null,
    showOngoingWatchBadge: Boolean? = null,
  ) {
    updateConfig { cfg ->
      val cur = cfg.launcherSettings
      cfg.copy(
        launcherSettings = cur.copy(
          keepAliveHomeMode = keepAliveHomeMode ?: cur.keepAliveHomeMode,
          ambientAlwaysOnEnabled = ambientAlwaysOnEnabled ?: cur.ambientAlwaysOnEnabled,
          autoLaunchOnBoot = autoLaunchOnBoot ?: cur.autoLaunchOnBoot,
          showOngoingWatchBadge = showOngoingWatchBadge ?: cur.showOngoingWatchBadge,
        ),
      )
    }
  }

  fun launchWatchHomeFromPhone() {
    viewModelScope.launch {
      refreshWearNodes()
      repository.sendLaunchHomeToWatch()
        .onSuccess { showMessage(it) }
        .onFailure { showMessage(it.localizedMessage ?: "Watch起動コマンド送信失敗") }
    }
  }

  fun forceRefreshWatchUi() {
    viewModelScope.launch {
      refreshWearNodes()
      repository.sendForceRefreshToWatch()
        .onSuccess { showMessage(it) }
        .onFailure { showMessage(it.localizedMessage ?: "Watchリフレッシュ失敗") }
    }
  }

  fun launchRemoteWatchUpdate(customReleaseUrl: String? = null) {
    if (_uiState.value.isLaunchingRemoteUpdate) return
    _uiState.update { it.copy(isLaunchingRemoteUpdate = true) }
    viewModelScope.launch {
      val primaryNodeId = _uiState.value.connectedNodes.firstOrNull { it.hasCompanionApp }?.id
        ?: _uiState.value.connectedNodes.firstOrNull()?.id
      val result = repository.launchRemoteWatchUpdate(primaryNodeId, customReleaseUrl)
      _uiState.update {
        it.copy(
          isLaunchingRemoteUpdate = false,
          snackbarMessage = result.getOrElse { err -> err.localizedMessage ?: "Watch更新起動に失敗しました" },
        )
      }
    }
  }

  fun refreshWearNodes() {
    _uiState.update { it.copy(isScanningNodes = true) }
    viewModelScope.launch {
      val (available, nodes) = repository.scanWearNodes()
      val watchStatus = repository.queryWatchAppStatus(nodes)
      _uiState.update { state ->
        state.copy(
          wearApiAvailable = available,
          connectedNodes = nodes,
          watchAppStatus = watchStatus ?: state.watchAppStatus,
          isScanningNodes = false,
        )
      }
    }
  }

  fun syncNowToPixelWatch() {
    if (_uiState.value.isSyncing) return
    _uiState.update { it.copy(isSyncing = true) }
    viewModelScope.launch {
      val result = repository.syncToPixelWatch(_uiState.value.activeConfig)
      val msg = when (result) {
        is WearSyncResult.SyncedToWatch -> result.message
        is WearSyncResult.NoPairedWatchFound -> result.reason
        is WearSyncResult.Error -> result.message
      }
      _uiState.update {
        it.copy(
          isSyncing = false,
          lastSyncResult = result,
          lastOtaSyncMs = if (result is WearSyncResult.SyncedToWatch) System.currentTimeMillis() else it.lastOtaSyncMs,
          snackbarMessage = msg,
        )
      }
      refreshWearNodes()
    }
  }

  fun showMessage(msg: String) {
    _uiState.update { it.copy(snackbarMessage = msg) }
  }

  fun clearSnackbar() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }
}
