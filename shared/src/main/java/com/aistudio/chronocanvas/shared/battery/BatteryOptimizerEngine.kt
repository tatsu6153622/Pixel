package com.aistudio.chronocanvas.shared.battery

import com.aistudio.chronocanvas.shared.model.BatteryProfile
import com.aistudio.chronocanvas.shared.model.ClockStyleId
import com.aistudio.chronocanvas.shared.model.ShaderPresetId
import com.aistudio.chronocanvas.shared.model.WallpaperSourceType
import com.aistudio.chronocanvas.shared.model.WatchHomeConfig
import com.aistudio.chronocanvas.shared.registry.PLUGIN_NONE
import com.aistudio.chronocanvas.shared.registry.WatchExtensionRegistry
import kotlin.math.roundToInt

data class BatteryPowerReport(
  val averageDrainMa: Float,
  val videoDecodeMa: Float,
  val oledDisplayMa: Float,
  val sensorPollingMa: Float,
  val ambientBaselineMa: Float,
  val estimatedHours: Float,
  val efficiencyScore: Int,
  val optimizationTips: List<String>,
)

object BatteryOptimizerEngine {

  fun applyProfile(
    current: WatchHomeConfig,
    profile: BatteryProfile,
  ): WatchHomeConfig {
    return when (profile) {
      BatteryProfile.ULTRA_ENDURANCE -> current.copy(
        clockStyle = current.clockStyle.copy(
          smoothSweepSeconds = false,
          ambientMonochrome = true,
        ),
        wallpaper = current.wallpaper.copy(
          dimOpacity = 0.65f,
          targetFps = 15,
          downscalePx = 360,
          pauseInAmbient = true,
        ),
        battery = current.battery.copy(
          profile = BatteryProfile.ULTRA_ENDURANCE,
          pauseVideoInAod = true,
          ltpoAmbient1Hz = true,
          maxVideoFps = 15,
          oledBlackBoost = 0.65f,
          complicationRefreshSec = 120,
          syncOnlyOnCharger = true,
        ),
      )

      BatteryProfile.BALANCED -> current.copy(
        clockStyle = current.clockStyle.copy(
          smoothSweepSeconds = true,
          ambientMonochrome = true,
        ),
        wallpaper = current.wallpaper.copy(
          dimOpacity = 0.35f,
          targetFps = 24,
          downscalePx = current.watchModel.nativePx,
          pauseInAmbient = true,
        ),
        battery = current.battery.copy(
          profile = BatteryProfile.BALANCED,
          pauseVideoInAod = true,
          ltpoAmbient1Hz = true,
          maxVideoFps = 24,
          oledBlackBoost = 0.35f,
          complicationRefreshSec = 30,
          syncOnlyOnCharger = false,
        ),
      )

      BatteryProfile.MAX_VISUAL -> current.copy(
        clockStyle = current.clockStyle.copy(
          smoothSweepSeconds = true,
          ambientMonochrome = false,
        ),
        wallpaper = current.wallpaper.copy(
          dimOpacity = 0.15f,
          targetFps = 30,
          downscalePx = current.watchModel.nativePx,
          pauseInAmbient = false,
        ),
        battery = current.battery.copy(
          profile = BatteryProfile.MAX_VISUAL,
          pauseVideoInAod = false,
          ltpoAmbient1Hz = false,
          maxVideoFps = 30,
          oledBlackBoost = 0.15f,
          complicationRefreshSec = 15,
          syncOnlyOnCharger = false,
        ),
      )

      BatteryProfile.CUSTOM -> current.copy(
        battery = current.battery.copy(profile = BatteryProfile.CUSTOM),
      )
    }
  }

  fun analyze(config: WatchHomeConfig): BatteryPowerReport {
    val bat = config.battery
    val wp = config.wallpaper

    val rawVideoMa = when (wp.sourceType) {
      WallpaperSourceType.LOCAL_VIDEO_URI -> {
        val fpsFactor = (wp.targetFps.coerceAtMost(bat.maxVideoFps) / 30f)
        val resFactor = (wp.downscalePx / 456f).let { it * it }
        val activeDecode = 38f * fpsFactor * resFactor
        val aodPenalty = if (!bat.pauseVideoInAod || !wp.pauseInAmbient) 18f else 0f
        activeDecode * 0.14f + aodPenalty
      }

      WallpaperSourceType.PROCEDURAL_SHADER -> {
        if (wp.shaderPresetId == ShaderPresetId.PURE_OLED_CARBON) 0.4f
        else 1.8f + (if (!bat.pauseVideoInAod) 3.5f else 0f)
      }

      WallpaperSourceType.PRESET_ART -> 0.6f
    }

    val dimMask = maxOf(wp.dimOpacity, bat.oledBlackBoost).coerceIn(0f, 0.9f)
    val baseOledActive = when (wp.sourceType) {
      WallpaperSourceType.PROCEDURAL_SHADER ->
        if (wp.shaderPresetId == ShaderPresetId.PURE_OLED_CARBON) 2.5f else 9.0f
      WallpaperSourceType.PRESET_ART -> 10.5f
      WallpaperSourceType.LOCAL_VIDEO_URI -> 14.0f
    }
    val oledMa = (baseOledActive * (1f - dimMask * 0.72f)) * 0.25f +
      if (config.clockStyle.styleId == ClockStyleId.MODULAR_MINIMAL) 0.6f else 1.2f

    val activeSlots = config.widgetSlots.filter { it.pluginId != PLUGIN_NONE }
    val minRefresh = activeSlots.minOfOrNull { slot ->
      WatchExtensionRegistry.findPlugin(slot.pluginId, config.customExtensions).defaultRefreshSec
    } ?: 3600
    val effectiveRefresh = maxOf(minRefresh, bat.complicationRefreshSec)
    val sensorMa = if (activeSlots.isEmpty()) 0.1f else {
      (60f / effectiveRefresh.coerceAtLeast(10)) * 0.85f +
        (if (config.clockStyle.showSeconds && config.clockStyle.smoothSweepSeconds) 0.8f else 0.2f)
    }

    val ambientMa = if (bat.ltpoAmbient1Hz) {
      if (config.clockStyle.ambientMonochrome) 3.2f else 4.5f
    } else {
      8.8f
    }

    val systemStandbyMa = 3.8f
    val totalMa = (rawVideoMa + oledMa + sensorMa + ambientMa + systemStandbyMa).coerceAtLeast(4.5f)
    val capacity = config.watchModel.batteryCapacityMah.toFloat()
    val estimatedHours = (capacity / totalMa).coerceIn(6f, 72f)
    val score = ((estimatedHours / 48f) * 100f).roundToInt().coerceIn(12, 99)

    val tips = buildList {
      if (wp.sourceType == WallpaperSourceType.LOCAL_VIDEO_URI && !bat.pauseVideoInAod) {
        add("AOD中の動画再生がONです。「AOD時は動画を自動一時停止」を有効にすると最大35%節電できます。")
      }
      if (wp.sourceType == WallpaperSourceType.LOCAL_VIDEO_URI && wp.targetFps > 24) {
        add("動画フレームレートを24fpsまたは15fpsに落とすとデコーダ負荷を大幅に削減できます。")
      }
      if (dimMask < 0.30f) {
        add("OLED黒マスク減光を35%以上に設定すると、有機ELの点灯電力を効率よく抑えられます。")
      }
      if (!bat.ltpoAmbient1Hz) {
        add("LTPO 1Hzアンビエント制御を有効化すると、Pixel Watch 3の待機電力を最小化できます。")
      }
      if (activeSlots.isNotEmpty() && bat.complicationRefreshSec < 30) {
        add("ウィジェット更新間隔を30秒〜60秒にするとセンサーのウェイクアップ回数を減らせます。")
      }
      if (isEmpty()) {
        add("現在の構成はPixel Watch 3のLTPO OLED特性に対して最適にチューニングされています。")
      }
    }

    return BatteryPowerReport(
      averageDrainMa = totalMa,
      videoDecodeMa = rawVideoMa,
      oledDisplayMa = oledMa,
      sensorPollingMa = sensorMa,
      ambientBaselineMa = ambientMa,
      estimatedHours = estimatedHours,
      efficiencyScore = score,
      optimizationTips = tips,
    )
  }
}
