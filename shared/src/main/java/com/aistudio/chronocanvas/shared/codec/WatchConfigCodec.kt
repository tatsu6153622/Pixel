package com.aistudio.chronocanvas.shared.codec

import com.aistudio.chronocanvas.shared.model.AppDockShortcut
import com.aistudio.chronocanvas.shared.model.BatteryOptimizationConfig
import com.aistudio.chronocanvas.shared.model.BatteryProfile
import com.aistudio.chronocanvas.shared.model.ClockFontWeightLevel
import com.aistudio.chronocanvas.shared.model.ClockStyleConfig
import com.aistudio.chronocanvas.shared.model.ClockStyleId
import com.aistudio.chronocanvas.shared.model.CustomWidgetExtension
import com.aistudio.chronocanvas.shared.model.DateDisplayFormat
import com.aistudio.chronocanvas.shared.model.LauncherSettingsConfig
import com.aistudio.chronocanvas.shared.model.NotificationAlertConfig
import com.aistudio.chronocanvas.shared.model.NotificationEdgeStyle
import com.aistudio.chronocanvas.shared.model.ShaderPresetId
import com.aistudio.chronocanvas.shared.model.TickStyle
import com.aistudio.chronocanvas.shared.model.TimePeriodSlot
import com.aistudio.chronocanvas.shared.model.TimeWallpaperRule
import com.aistudio.chronocanvas.shared.model.WallpaperAutomationConfig
import com.aistudio.chronocanvas.shared.model.WallpaperConfig
import com.aistudio.chronocanvas.shared.model.WallpaperSourceType
import com.aistudio.chronocanvas.shared.model.WatchHomeConfig
import com.aistudio.chronocanvas.shared.model.WatchModelSpec
import com.aistudio.chronocanvas.shared.model.WidgetSlotConfig
import com.aistudio.chronocanvas.shared.model.WidgetSlotPosition

object WatchConfigCodec {

  const val SCHEMA_VERSION = 4

  fun toJson(config: WatchHomeConfig, pretty: Boolean = false): String {
    val root = linkedMapOf<String, Any?>(
      "schemaVersion" to SCHEMA_VERSION,
      "id" to config.id,
      "name" to config.name,
      "watchModel" to config.watchModel.name,
      "showAppDockButton" to config.showAppDockButton,
      "updatedAtEpochMs" to config.updatedAtEpochMs,
      "clockStyle" to linkedMapOf(
        "styleId" to config.clockStyle.styleId.name,
        "accentColorHex" to config.clockStyle.accentColorHex,
        "secondaryColorHex" to config.clockStyle.secondaryColorHex,
        "hourTextColorHex" to config.clockStyle.hourTextColorHex,
        "dateTextColorHex" to config.clockStyle.dateTextColorHex,
        "showSeconds" to config.clockStyle.showSeconds,
        "smoothSweepSeconds" to config.clockStyle.smoothSweepSeconds,
        "use24Hour" to config.clockStyle.use24Hour,
        "clockScale" to config.clockStyle.clockScale.toDouble(),
        "clockOpacity" to config.clockStyle.clockOpacity.toDouble(),
        "offsetXFraction" to config.clockStyle.offsetXFraction.toDouble(),
        "offsetYFraction" to config.clockStyle.offsetYFraction.toDouble(),
        "fontWeight" to config.clockStyle.fontWeight.name,
        "dateFormat" to config.clockStyle.dateFormat.name,
        "showModelTag" to config.clockStyle.showModelTag,
        "ambientMonochrome" to config.clockStyle.ambientMonochrome,
        "tickStyle" to config.clockStyle.tickStyle.name,
      ),
      "wallpaper" to linkedMapOf(
        "sourceType" to config.wallpaper.sourceType.name,
        "presetArtId" to config.wallpaper.presetArtId,
        "shaderPresetId" to config.wallpaper.shaderPresetId.name,
        "videoUriString" to config.wallpaper.videoUriString,
        "videoTitle" to config.wallpaper.videoTitle,
        "loopEnabled" to config.wallpaper.loopEnabled,
        "playbackSpeed" to config.wallpaper.playbackSpeed.toDouble(),
        "dimOpacity" to config.wallpaper.dimOpacity.toDouble(),
        "targetFps" to config.wallpaper.targetFps,
        "downscalePx" to config.wallpaper.downscalePx,
        "pauseInAmbient" to config.wallpaper.pauseInAmbient,
      ),
      "wallpaperAutomation" to linkedMapOf(
        "switchOnOffWrist" to config.wallpaperAutomation.switchOnOffWrist,
        "offWristSourceType" to config.wallpaperAutomation.offWristSourceType.name,
        "offWristShaderPreset" to config.wallpaperAutomation.offWristShaderPreset.name,
        "offWristPresetArtId" to config.wallpaperAutomation.offWristPresetArtId,
        "offWristVideoUriString" to config.wallpaperAutomation.offWristVideoUriString,
        "offWristVideoTitle" to config.wallpaperAutomation.offWristVideoTitle,
        "offWristDimOpacity" to config.wallpaperAutomation.offWristDimOpacity.toDouble(),
        "timeScheduleEnabled" to config.wallpaperAutomation.timeScheduleEnabled,
        "timeRules" to config.wallpaperAutomation.timeRules.map { rule ->
          linkedMapOf(
            "period" to rule.period.name,
            "enabled" to rule.enabled,
            "sourceType" to rule.sourceType.name,
            "presetArtId" to rule.presetArtId,
            "shaderPresetId" to rule.shaderPresetId.name,
            "videoUriString" to rule.videoUriString,
            "videoTitle" to rule.videoTitle,
            "dimOpacity" to rule.dimOpacity.toDouble(),
          )
        },
      ),
      "notificationAlert" to linkedMapOf(
        "edgeGlowEnabled" to config.notificationAlert.edgeGlowEnabled,
        "edgeGlowColorHex" to config.notificationAlert.edgeGlowColorHex,
        "edgeGlowStyle" to config.notificationAlert.edgeGlowStyle.name,
        "edgeGlowThicknessDp" to config.notificationAlert.edgeGlowThicknessDp.toDouble(),
        "glowWhileUnread" to config.notificationAlert.glowWhileUnread,
        "flashDurationSec" to config.notificationAlert.flashDurationSec,
      ),
      "launcherSettings" to linkedMapOf(
        "keepAliveHomeMode" to config.launcherSettings.keepAliveHomeMode,
        "ambientAlwaysOnEnabled" to config.launcherSettings.ambientAlwaysOnEnabled,
        "autoLaunchOnBoot" to config.launcherSettings.autoLaunchOnBoot,
        "showOngoingWatchBadge" to config.launcherSettings.showOngoingWatchBadge,
      ),
      "widgetSlots" to config.widgetSlots.map { slot ->
        linkedMapOf(
          "position" to slot.position.name,
          "pluginId" to slot.pluginId,
          "customTitle" to slot.customTitle,
          "customValue" to slot.customValue,
          "showInAmbient" to slot.showInAmbient,
          "accentOverrideHex" to slot.accentOverrideHex,
          "offsetXFraction" to slot.offsetXFraction.toDouble(),
          "offsetYFraction" to slot.offsetYFraction.toDouble(),
          "scale" to slot.scale.toDouble(),
          "backgroundAlpha" to slot.backgroundAlpha.toDouble(),
        )
      },
      "appDockShortcuts" to config.appDockShortcuts.map { app ->
        linkedMapOf(
          "id" to app.id,
          "label" to app.label,
          "iconKey" to app.iconKey,
          "packageHint" to app.packageHint,
          "enabled" to app.enabled,
        )
      },
      "battery" to linkedMapOf(
        "profile" to config.battery.profile.name,
        "pauseVideoInAod" to config.battery.pauseVideoInAod,
        "ltpoAmbient1Hz" to config.battery.ltpoAmbient1Hz,
        "maxVideoFps" to config.battery.maxVideoFps,
        "oledBlackBoost" to config.battery.oledBlackBoost.toDouble(),
        "complicationRefreshSec" to config.battery.complicationRefreshSec,
        "syncOnlyOnCharger" to config.battery.syncOnlyOnCharger,
        "autoSaverThresholdPct" to config.battery.autoSaverThresholdPct,
      ),
      "customExtensions" to config.customExtensions.map { ext ->
        linkedMapOf(
          "pluginId" to ext.pluginId,
          "title" to ext.title,
          "shortTag" to ext.shortTag,
          "defaultValue" to ext.defaultValue,
          "unit" to ext.unit,
          "iconKey" to ext.iconKey,
          "category" to ext.category,
          "refreshIntervalSec" to ext.refreshIntervalSec,
          "supportsAmbient" to ext.supportsAmbient,
        )
      },
    )
    return buildString {
      writeJsonValue(root, this, pretty, 0)
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun fromJson(json: String): WatchHomeConfig {
    val root = JsonParser(json).parseValue() as? Map<String, Any?>
      ?: throw IllegalArgumentException("Root JSON must be an object")

    val clockObj = root["clockStyle"] as? Map<String, Any?>
    val wpObj = root["wallpaper"] as? Map<String, Any?>
    val autoObj = root["wallpaperAutomation"] as? Map<String, Any?>
    val notifObj = root["notificationAlert"] as? Map<String, Any?>
    val launcherObj = root["launcherSettings"] as? Map<String, Any?>
    val batObj = root["battery"] as? Map<String, Any?>

    val slots = (root["widgetSlots"] as? List<*>)?.mapNotNull { item ->
      val obj = item as? Map<String, Any?> ?: return@mapNotNull null
      val pos = runCatching {
        WidgetSlotPosition.valueOf(obj.str("position"))
      }.getOrNull() ?: return@mapNotNull null
      WidgetSlotConfig(
        position = pos,
        pluginId = obj.str("pluginId", "sys_battery"),
        customTitle = obj.strOrNull("customTitle"),
        customValue = obj.strOrNull("customValue"),
        showInAmbient = obj.bool("showInAmbient", true),
        accentOverrideHex = obj.longOrNull("accentOverrideHex"),
        offsetXFraction = obj.float("offsetXFraction", pos.defaultX),
        offsetYFraction = obj.float("offsetYFraction", pos.defaultY),
        scale = obj.float("scale", 1.0f),
        backgroundAlpha = obj.float("backgroundAlpha", 0.72f),
      )
    }.orEmpty()

    val timeRules = (autoObj?.get("timeRules") as? List<*>)?.mapNotNull { item ->
      val obj = item as? Map<String, Any?> ?: return@mapNotNull null
      val period = runCatching { TimePeriodSlot.valueOf(obj.str("period")) }.getOrNull()
        ?: return@mapNotNull null
      TimeWallpaperRule(
        period = period,
        enabled = obj.bool("enabled", false),
        sourceType = enumOrDefault(obj.str("sourceType"), WallpaperSourceType.PROCEDURAL_SHADER),
        presetArtId = obj.str("presetArtId", "preset_aurora"),
        shaderPresetId = enumOrDefault(obj.str("shaderPresetId"), ShaderPresetId.AURORA_WAVE),
        videoUriString = obj.str("videoUriString", ""),
        videoTitle = obj.str("videoTitle", "未選択"),
        dimOpacity = obj.float("dimOpacity", 0.35f),
      )
    }.orEmpty()

    val dock = (root["appDockShortcuts"] as? List<*>)?.mapNotNull { item ->
      val obj = item as? Map<String, Any?> ?: return@mapNotNull null
      AppDockShortcut(
        id = obj.str("id"),
        label = obj.str("label"),
        iconKey = obj.str("iconKey", "spark"),
        packageHint = obj.str("packageHint"),
        enabled = obj.bool("enabled", true),
      )
    }.orEmpty()

    val extensions = (root["customExtensions"] as? List<*>)?.mapNotNull { item ->
      val obj = item as? Map<String, Any?> ?: return@mapNotNull null
      CustomWidgetExtension(
        pluginId = obj.str("pluginId"),
        title = obj.str("title"),
        shortTag = obj.str("shortTag"),
        defaultValue = obj.str("defaultValue"),
        unit = obj.str("unit"),
        iconKey = obj.str("iconKey", "spark"),
        category = obj.str("category", "カスタム拡張"),
        refreshIntervalSec = obj.int("refreshIntervalSec", 60),
        supportsAmbient = obj.bool("supportsAmbient", true),
      )
    }.orEmpty()

    return WatchHomeConfig(
      id = root.str("id", "active_config"),
      name = root.str("name", "Pixel Watch 3 Studio"),
      watchModel = enumOrDefault(root.str("watchModel"), WatchModelSpec.PIXEL_WATCH_3_45MM),
      showAppDockButton = root.bool("showAppDockButton", true),
      clockStyle = if (clockObj != null) {
        ClockStyleConfig(
          styleId = enumOrDefault(clockObj.str("styleId"), ClockStyleId.ORBIT_DIGITAL),
          accentColorHex = clockObj.long("accentColorHex", 0xFF3EF0D0),
          secondaryColorHex = clockObj.long("secondaryColorHex", 0xFFFFB84D),
          hourTextColorHex = clockObj.long("hourTextColorHex", 0xFFFFFFFF),
          dateTextColorHex = clockObj.long("dateTextColorHex", 0xFFD0DAE8),
          showSeconds = clockObj.bool("showSeconds", true),
          smoothSweepSeconds = clockObj.bool("smoothSweepSeconds", true),
          use24Hour = clockObj.bool("use24Hour", true),
          clockScale = clockObj.float("clockScale", 1.0f),
          clockOpacity = clockObj.float("clockOpacity", 1.0f),
          offsetXFraction = clockObj.float("offsetXFraction", 0f),
          offsetYFraction = clockObj.float("offsetYFraction", 0f),
          fontWeight = enumOrDefault(clockObj.str("fontWeight"), ClockFontWeightLevel.BOLD),
          dateFormat = enumOrDefault(clockObj.str("dateFormat"), DateDisplayFormat.SHORT_EN),
          showModelTag = clockObj.bool("showModelTag", false),
          ambientMonochrome = clockObj.bool("ambientMonochrome", true),
          tickStyle = enumOrDefault(clockObj.str("tickStyle"), TickStyle.PRECISION_60),
        )
      } else {
        ClockStyleConfig()
      },
      wallpaper = if (wpObj != null) {
        WallpaperConfig(
          sourceType = enumOrDefault(wpObj.str("sourceType"), WallpaperSourceType.PROCEDURAL_SHADER),
          presetArtId = wpObj.str("presetArtId", "preset_aurora"),
          shaderPresetId = enumOrDefault(wpObj.str("shaderPresetId"), ShaderPresetId.AURORA_WAVE),
          videoUriString = wpObj.str("videoUriString", ""),
          videoTitle = wpObj.str("videoTitle", "未選択"),
          loopEnabled = wpObj.bool("loopEnabled", true),
          playbackSpeed = wpObj.float("playbackSpeed", 1.0f),
          dimOpacity = wpObj.float("dimOpacity", 0.35f),
          targetFps = wpObj.int("targetFps", 24),
          downscalePx = wpObj.int("downscalePx", 456),
          pauseInAmbient = wpObj.bool("pauseInAmbient", true),
        )
      } else {
        WallpaperConfig()
      },
      wallpaperAutomation = if (autoObj != null) {
        WallpaperAutomationConfig(
          switchOnOffWrist = autoObj.bool("switchOnOffWrist", false),
          offWristSourceType = enumOrDefault(autoObj.str("offWristSourceType"), WallpaperSourceType.PROCEDURAL_SHADER),
          offWristShaderPreset = enumOrDefault(autoObj.str("offWristShaderPreset"), ShaderPresetId.PURE_OLED_CARBON),
          offWristPresetArtId = autoObj.str("offWristPresetArtId", "preset_horology"),
          offWristVideoUriString = autoObj.str("offWristVideoUriString", ""),
          offWristVideoTitle = autoObj.str("offWristVideoTitle", "未選択"),
          offWristDimOpacity = autoObj.float("offWristDimOpacity", 0.65f),
          timeScheduleEnabled = autoObj.bool("timeScheduleEnabled", false),
          timeRules = timeRules.ifEmpty { WallpaperAutomationConfig.defaultTimeRules() },
        )
      } else {
        WallpaperAutomationConfig()
      },
      notificationAlert = if (notifObj != null) {
        NotificationAlertConfig(
          edgeGlowEnabled = notifObj.bool("edgeGlowEnabled", true),
          edgeGlowColorHex = notifObj.long("edgeGlowColorHex", 0xFF3EF0D0),
          edgeGlowStyle = enumOrDefault(notifObj.str("edgeGlowStyle"), NotificationEdgeStyle.PULSE_RING),
          edgeGlowThicknessDp = notifObj.float("edgeGlowThicknessDp", 4.5f),
          glowWhileUnread = notifObj.bool("glowWhileUnread", true),
          flashDurationSec = notifObj.int("flashDurationSec", 6),
        )
      } else {
        NotificationAlertConfig()
      },
      launcherSettings = if (launcherObj != null) {
        LauncherSettingsConfig(
          keepAliveHomeMode = launcherObj.bool("keepAliveHomeMode", true),
          ambientAlwaysOnEnabled = launcherObj.bool("ambientAlwaysOnEnabled", true),
          autoLaunchOnBoot = launcherObj.bool("autoLaunchOnBoot", true),
          showOngoingWatchBadge = launcherObj.bool("showOngoingWatchBadge", true),
        )
      } else {
        LauncherSettingsConfig()
      },
      widgetSlots = slots.ifEmpty { WatchHomeConfig.defaultWidgetSlots() },
      appDockShortcuts = dock.ifEmpty { WatchHomeConfig.defaultAppDockShortcuts() },
      battery = if (batObj != null) {
        BatteryOptimizationConfig(
          profile = enumOrDefault(batObj.str("profile"), BatteryProfile.BALANCED),
          pauseVideoInAod = batObj.bool("pauseVideoInAod", true),
          ltpoAmbient1Hz = batObj.bool("ltpoAmbient1Hz", true),
          maxVideoFps = batObj.int("maxVideoFps", 24),
          oledBlackBoost = batObj.float("oledBlackBoost", 0.35f),
          complicationRefreshSec = batObj.int("complicationRefreshSec", 30),
          syncOnlyOnCharger = batObj.bool("syncOnlyOnCharger", false),
          autoSaverThresholdPct = batObj.int("autoSaverThresholdPct", 20),
        )
      } else {
        BatteryOptimizationConfig()
      },
      customExtensions = extensions,
      updatedAtEpochMs = root.long("updatedAtEpochMs", 0L),
    )
  }

  private fun writeJsonValue(value: Any?, sb: StringBuilder, pretty: Boolean, indent: Int) {
    when (value) {
      null -> sb.append("null")
      is Boolean -> sb.append(if (value) "true" else "false")
      is Number -> sb.append(value.toString())
      is String -> writeEscapedString(value, sb)
      is Map<*, *> -> {
        if (value.isEmpty()) {
          sb.append("{}")
          return
        }
        sb.append('{')
        var first = true
        for ((k, v) in value) {
          if (!first) sb.append(',')
          first = false
          if (pretty) sb.append('\n').append("  ".repeat(indent + 1))
          writeEscapedString(k.toString(), sb)
          sb.append(if (pretty) ": " else ":")
          writeJsonValue(v, sb, pretty, indent + 1)
        }
        if (pretty) sb.append('\n').append("  ".repeat(indent))
        sb.append('}')
      }
      is Iterable<*> -> {
        val list = value.toList()
        if (list.isEmpty()) {
          sb.append("[]")
          return
        }
        sb.append('[')
        list.forEachIndexed { idx, item ->
          if (idx > 0) sb.append(',')
          if (pretty) sb.append('\n').append("  ".repeat(indent + 1))
          writeJsonValue(item, sb, pretty, indent + 1)
        }
        if (pretty) sb.append('\n').append("  ".repeat(indent))
        sb.append(']')
      }
      else -> writeEscapedString(value.toString(), sb)
    }
  }

  private fun writeEscapedString(text: String, sb: StringBuilder) {
    sb.append('"')
    for (ch in text) {
      when (ch) {
        '"' -> sb.append("\\\"")
        '\\' -> sb.append("\\\\")
        '\n' -> sb.append("\\n")
        '\r' -> sb.append("\\r")
        '\t' -> sb.append("\\t")
        '\b' -> sb.append("\\b")
        '\u000C' -> sb.append("\\f")
        else -> if (ch.code < 0x20) {
          sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
        } else {
          sb.append(ch)
        }
      }
    }
    sb.append('"')
  }

  private class JsonParser(private val src: String) {
    private var idx = 0

    fun parseValue(): Any? {
      skipWs()
      if (idx >= src.length) throw IllegalArgumentException("Unexpected end of JSON")
      return when (val ch = src[idx]) {
        '{' -> parseObject()
        '[' -> parseArray()
        '"' -> parseString()
        't' -> { expect("true"); true }
        'f' -> { expect("false"); false }
        'n' -> { expect("null"); null }
        '-', in '0'..'9' -> parseNumber()
        else -> throw IllegalArgumentException("Unexpected char '$ch' at $idx")
      }
    }

    private fun parseObject(): Map<String, Any?> {
      idx++
      val map = linkedMapOf<String, Any?>()
      skipWs()
      if (peek() == '}') {
        idx++
        return map
      }
      while (true) {
        skipWs()
        val key = parseString()
        skipWs()
        if (peek() != ':') throw IllegalArgumentException("Expected ':' at $idx")
        idx++
        val value = parseValue()
        map[key] = value
        skipWs()
        when (peek()) {
          ',' -> idx++
          '}' -> {
            idx++
            return map
          }
          else -> throw IllegalArgumentException("Expected ',' or '}' at $idx")
        }
      }
    }

    private fun parseArray(): List<Any?> {
      idx++
      val list = mutableListOf<Any?>()
      skipWs()
      if (peek() == ']') {
        idx++
        return list
      }
      while (true) {
        list.add(parseValue())
        skipWs()
        when (peek()) {
          ',' -> idx++
          ']' -> {
            idx++
            return list
          }
          else -> throw IllegalArgumentException("Expected ',' or ']' at $idx")
        }
      }
    }

    private fun parseString(): String {
      if (peek() != '"') throw IllegalArgumentException("Expected '\"' at $idx")
      idx++
      val sb = StringBuilder()
      while (idx < src.length) {
        val ch = src[idx++]
        when (ch) {
          '"' -> return sb.toString()
          '\\' -> {
            if (idx >= src.length) throw IllegalArgumentException("Unterminated escape")
            when (val esc = src[idx++]) {
              '"' -> sb.append('"')
              '\\' -> sb.append('\\')
              '/' -> sb.append('/')
              'n' -> sb.append('\n')
              'r' -> sb.append('\r')
              't' -> sb.append('\t')
              'b' -> sb.append('\b')
              'f' -> sb.append('\u000C')
              'u' -> {
                val hex = src.substring(idx, (idx + 4).coerceAtMost(src.length))
                sb.append(hex.toInt(16).toChar())
                idx += 4
              }
              else -> sb.append(esc)
            }
          }
          else -> sb.append(ch)
        }
      }
      throw IllegalArgumentException("Unterminated string")
    }

    private fun parseNumber(): Number {
      val start = idx
      if (src[idx] == '-') idx++
      while (idx < src.length && src[idx] in '0'..'9') idx++
      var isFloat = false
      if (idx < src.length && src[idx] == '.') {
        isFloat = true
        idx++
        while (idx < src.length && src[idx] in '0'..'9') idx++
      }
      if (idx < src.length && (src[idx] == 'e' || src[idx] == 'E')) {
        isFloat = true
        idx++
        if (idx < src.length && (src[idx] == '+' || src[idx] == '-')) idx++
        while (idx < src.length && src[idx] in '0'..'9') idx++
      }
      val token = src.substring(start, idx)
      return if (isFloat) token.toDouble() else token.toLong()
    }

    private fun expect(lit: String) {
      if (!src.startsWith(lit, idx)) throw IllegalArgumentException("Expected '$lit' at $idx")
      idx += lit.length
    }

    private fun skipWs() {
      while (idx < src.length && src[idx].isWhitespace()) idx++
    }

    private fun peek(): Char = if (idx < src.length) src[idx] else '\u0000'
  }

  private fun Map<String, Any?>.str(key: String, default: String = ""): String =
    (this[key] as? String) ?: default

  private fun Map<String, Any?>.strOrNull(key: String): String? =
    this[key] as? String

  private fun Map<String, Any?>.bool(key: String, default: Boolean): Boolean =
    (this[key] as? Boolean) ?: default

  private fun Map<String, Any?>.int(key: String, default: Int): Int =
    (this[key] as? Number)?.toInt() ?: default

  private fun Map<String, Any?>.long(key: String, default: Long): Long =
    (this[key] as? Number)?.toLong() ?: default

  private fun Map<String, Any?>.longOrNull(key: String): Long? =
    (this[key] as? Number)?.toLong()

  private fun Map<String, Any?>.float(key: String, default: Float): Float =
    (this[key] as? Number)?.toFloat() ?: default

  private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T {
    return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
  }
}
