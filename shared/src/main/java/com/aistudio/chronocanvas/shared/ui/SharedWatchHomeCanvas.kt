package com.aistudio.chronocanvas.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.chronocanvas.shared.model.AppDockShortcut
import com.aistudio.chronocanvas.shared.model.ClockFontWeightLevel
import com.aistudio.chronocanvas.shared.model.ClockStyleId
import com.aistudio.chronocanvas.shared.model.DateDisplayFormat
import com.aistudio.chronocanvas.shared.model.NotificationEdgeStyle
import com.aistudio.chronocanvas.shared.model.ShaderPresetId
import com.aistudio.chronocanvas.shared.model.TickStyle
import com.aistudio.chronocanvas.shared.model.WallpaperConfig
import com.aistudio.chronocanvas.shared.model.WallpaperSourceType
import com.aistudio.chronocanvas.shared.model.WatchHomeConfig
import com.aistudio.chronocanvas.shared.model.WatchModelSpec
import com.aistudio.chronocanvas.shared.model.WatchTelemetrySnapshot
import com.aistudio.chronocanvas.shared.model.WidgetSlotConfig
import com.aistudio.chronocanvas.shared.model.WidgetSlotPosition
import com.aistudio.chronocanvas.shared.registry.PLUGIN_NONE
import com.aistudio.chronocanvas.shared.registry.WatchExtensionRegistry
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun resolveEffectiveWallpaper(
  config: WatchHomeConfig,
  telemetry: WatchTelemetrySnapshot,
  hour24: Int,
): WallpaperConfig {
  val auto = config.wallpaperAutomation
  if (telemetry.isOffWrist && auto.switchOnOffWrist) {
    return config.wallpaper.copy(
      sourceType = auto.offWristSourceType,
      shaderPresetId = auto.offWristShaderPreset,
      presetArtId = auto.offWristPresetArtId,
      videoUriString = auto.offWristVideoUriString.ifBlank { config.wallpaper.videoUriString },
      videoTitle = auto.offWristVideoTitle,
      dimOpacity = auto.offWristDimOpacity,
    )
  }
  if (auto.timeScheduleEnabled) {
    val matching = auto.timeRules.firstOrNull { rule ->
      rule.enabled && hour24 >= rule.period.startHour && hour24 < rule.period.endHourExclusive
    }
    if (matching != null) {
      return config.wallpaper.copy(
        sourceType = matching.sourceType,
        presetArtId = matching.presetArtId,
        shaderPresetId = matching.shaderPresetId,
        videoUriString = matching.videoUriString.ifBlank { config.wallpaper.videoUriString },
        videoTitle = matching.videoTitle,
        dimOpacity = matching.dimOpacity,
      )
    }
  }
  return config.wallpaper
}

@Composable
fun SharedWatchHomeCanvas(
  config: WatchHomeConfig,
  telemetry: WatchTelemetrySnapshot,
  isAmbientMode: Boolean,
  showAppDock: Boolean,
  onToggleAppDock: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  selectedSlotForEdit: WidgetSlotPosition? = null,
  onSlotClick: ((WidgetSlotPosition) -> Unit)? = null,
  onAppShortcutClick: ((AppDockShortcut) -> Unit)? = null,
  showOuterBezel: Boolean = true,
  previewHourOverride: Int? = null,
  displayFontFamily: FontFamily = FontFamily.SansSerif,
  monoFontFamily: FontFamily = FontFamily.Monospace,
  videoWallpaperSlot: @Composable (WallpaperConfig, Boolean) -> Unit = { _, _ -> },
  presetArtworkSlot: @Composable (String, Boolean) -> Unit = { _, _ -> },
) {
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

  val isPulseActive = remember(nowMillis, telemetry.lastNotificationPulseMs, config.notificationAlert) {
    val elapsed = nowMillis - telemetry.lastNotificationPulseMs
    config.notificationAlert.edgeGlowEnabled &&
      ((elapsed in 0L..(config.notificationAlert.flashDurationSec * 1000L)) ||
        (config.notificationAlert.glowWhileUnread && telemetry.unreadNotificationCount > 0))
  }

  val tickIntervalMs = remember(
    isAmbientMode,
    isPulseActive,
    config.clockStyle.smoothSweepSeconds,
    config.wallpaper.sourceType,
    config.wallpaper.targetFps,
  ) {
    when {
      isAmbientMode -> 1000L
      isPulseActive -> 33L
      config.clockStyle.styleId == ClockStyleId.CHRONO_ANALOG && config.clockStyle.smoothSweepSeconds -> 33L
      config.wallpaper.sourceType == WallpaperSourceType.PROCEDURAL_SHADER ->
        (1000L / config.wallpaper.targetFps.coerceIn(10, 30)).coerceAtLeast(33L)
      else -> 250L
    }
  }

  LaunchedEffect(tickIntervalMs) {
    while (true) {
      nowMillis = System.currentTimeMillis()
      delay(tickIntervalMs)
    }
  }

  val calendar = remember(nowMillis) {
    Calendar.getInstance().apply { timeInMillis = nowMillis }
  }
  val hour24 = previewHourOverride ?: calendar.get(Calendar.HOUR_OF_DAY)
  val minute = calendar.get(Calendar.MINUTE)
  val second = calendar.get(Calendar.SECOND)
  val milli = calendar.get(Calendar.MILLISECOND)
  val year = calendar.get(Calendar.YEAR)
  val month = calendar.get(Calendar.MONTH) + 1
  val day = calendar.get(Calendar.DAY_OF_MONTH)
  val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
  val dateText = remember(year, month, day, dayOfWeek, config.clockStyle.dateFormat) {
    formatWatchDate(year, month, day, dayOfWeek, config.clockStyle.dateFormat)
  }

  val effectiveWallpaper = remember(config, telemetry.isOffWrist, hour24) {
    resolveEffectiveWallpaper(config, telemetry, hour24)
  }

  val ambientOverlayAlpha by animateFloatAsState(
    targetValue = if (isAmbientMode) 1f else 0f,
    animationSpec = tween(280),
    label = "ambientOverlay",
  )

  val rawAccent = Color(config.clockStyle.accentColorHex)
  val rawSecondary = Color(config.clockStyle.secondaryColorHex)
  val rawHourColor = Color(config.clockStyle.hourTextColorHex)
  val accentColor = if (isAmbientMode && config.clockStyle.ambientMonochrome) {
    Color(0xFFB5C0CE)
  } else {
    rawAccent
  }
  val secondaryColor = if (isAmbientMode && config.clockStyle.ambientMonochrome) {
    Color(0xFF7B8797)
  } else {
    rawSecondary
  }
  val hourColor = if (isAmbientMode && config.clockStyle.ambientMonochrome) {
    Color(0xFFD0D8E2)
  } else {
    rawHourColor
  }

  val clockWeight = when (config.clockStyle.fontWeight) {
    ClockFontWeightLevel.LIGHT -> FontWeight.Light
    ClockFontWeightLevel.MEDIUM -> FontWeight.Medium
    ClockFontWeightLevel.BOLD -> FontWeight.Bold
  }

  BoxWithConstraints(
    modifier = modifier
      .aspectRatio(1f)
      .semantics {
        contentDescription = "${config.watchModel.displayName} プレビュー"
      },
    contentAlignment = Alignment.Center,
  ) {
    val bezelSize = maxWidth
    val bezelPadding: Dp = if (showOuterBezel) (bezelSize * 0.055f) else 0.dp
    val screenSide = (bezelSize - bezelPadding * 2).coerceAtLeast(180.dp)
    val screenPx = with(LocalDensity.current) { screenSide.toPx() }
    val compactScale = (screenSide / 270.dp).coerceIn(0.68f, 1.35f)

    if (showOuterBezel) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                Color(0xFF1E2530),
                Color(0xFF0E1218),
                Color(0xFF040609),
              ),
            ),
          )
          .border(
            width = 2.dp,
            brush = Brush.linearGradient(
              colors = listOf(
                Color(0xFF3E4B61),
                Color(0xFF151B24),
                Color(0xFF2D3748),
              ),
            ),
            shape = CircleShape,
          ),
      )
    }

    // Active circular OLED screen
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(bezelPadding)
        .clip(CircleShape)
        .background(SharedWatchPalette.PureBlackOled),
      contentAlignment = Alignment.Center,
    ) {
      // 1. Background Wallpaper Layer
      val pauseBackground = isAmbientMode && (config.battery.pauseVideoInAod || effectiveWallpaper.pauseInAmbient)
      if (!pauseBackground) {
        when (effectiveWallpaper.sourceType) {
          WallpaperSourceType.LOCAL_VIDEO_URI -> {
            if (effectiveWallpaper.videoUriString.isNotBlank()) {
              videoWallpaperSlot(effectiveWallpaper, isAmbientMode)
            } else {
              ProceduralShaderBackground(
                shaderPreset = effectiveWallpaper.shaderPresetId,
                accentColor = accentColor,
                secondaryColor = secondaryColor,
                nowMillis = nowMillis,
                isAmbient = isAmbientMode,
              )
            }
          }

          WallpaperSourceType.PRESET_ART -> {
            presetArtworkSlot(effectiveWallpaper.presetArtId, isAmbientMode)
          }

          WallpaperSourceType.PROCEDURAL_SHADER -> {
            ProceduralShaderBackground(
              shaderPreset = effectiveWallpaper.shaderPresetId,
              accentColor = accentColor,
              secondaryColor = secondaryColor,
              nowMillis = nowMillis,
              isAmbient = isAmbientMode,
            )
          }
        }
      }

      // 2. OLED True-Black Dim Mask
      val effectiveDim = maxOf(effectiveWallpaper.dimOpacity, config.battery.oledBlackBoost).coerceIn(0f, 0.92f)
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Color.Black.copy(
              alpha = if (pauseBackground) 0.96f else effectiveDim.coerceAtLeast(ambientOverlayAlpha * 0.85f),
            ),
          ),
      )

      // 3. Clock Face & Ticks Layer (with fine X/Y offset & opacity)
      val clockOffset = IntOffset(
        x = (screenPx * config.clockStyle.offsetXFraction).roundToInt(),
        y = (screenPx * config.clockStyle.offsetYFraction).roundToInt(),
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .alpha(config.clockStyle.clockOpacity.coerceIn(0.25f, 1f)),
        contentAlignment = Alignment.Center,
      ) {
        when (config.clockStyle.styleId) {
          ClockStyleId.CHRONO_ANALOG -> AnalogClockLayer(
            hour24 = hour24,
            minute = minute,
            second = second,
            milli = milli,
            smoothSweep = config.clockStyle.smoothSweepSeconds && !isAmbientMode,
            showSeconds = config.clockStyle.showSeconds && !isAmbientMode,
            tickStyle = config.clockStyle.tickStyle,
            accentColor = accentColor,
            secondaryColor = secondaryColor,
            handColor = hourColor,
            isAmbient = isAmbientMode,
            scale = config.clockStyle.clockScale,
            clockOffset = clockOffset,
          )

          ClockStyleId.ORBIT_DIGITAL -> OrbitDigitalClockLayer(
            hour24 = hour24,
            minute = minute,
            second = second,
            milli = milli,
            dateText = dateText,
            use24Hour = config.clockStyle.use24Hour,
            showSeconds = config.clockStyle.showSeconds && !isAmbientMode,
            tickStyle = config.clockStyle.tickStyle,
            accentColor = accentColor,
            secondaryColor = secondaryColor,
            hourColor = hourColor,
            isAmbient = isAmbientMode,
            scale = config.clockStyle.clockScale,
            fontWeight = clockWeight,
            clockOffset = clockOffset,
            displayFont = displayFontFamily,
            monoFont = monoFontFamily,
          )

          ClockStyleId.MODULAR_MINIMAL -> ModularMinimalClockLayer(
            hour24 = hour24,
            minute = minute,
            dateText = dateText,
            use24Hour = config.clockStyle.use24Hour,
            accentColor = accentColor,
            hourColor = hourColor,
            isAmbient = isAmbientMode,
            scale = config.clockStyle.clockScale,
            fontWeight = clockWeight,
            clockOffset = clockOffset,
            displayFont = displayFontFamily,
            monoFont = monoFontFamily,
          )

          ClockStyleId.DATA_MATRIX -> DataMatrixClockLayer(
            hour24 = hour24,
            minute = minute,
            second = second,
            dateText = dateText,
            modelTag = if (!config.clockStyle.showModelTag) null
            else if (config.watchModel == WatchModelSpec.PIXEL_WATCH_3_45MM) "PW3·45" else "PW3·41",
            use24Hour = config.clockStyle.use24Hour,
            showSeconds = config.clockStyle.showSeconds && !isAmbientMode,
            accentColor = accentColor,
            secondaryColor = secondaryColor,
            hourColor = hourColor,
            isAmbient = isAmbientMode,
            scale = config.clockStyle.clockScale,
            fontWeight = clockWeight,
            clockOffset = clockOffset,
            monoFont = monoFontFamily,
          )
        }
      }

      // 4. Modular Widget Complication Slots (Freely positioned, skippable via PLUGIN_NONE)
      config.widgetSlots.forEach { slot ->
        if (slot.pluginId != PLUGIN_NONE && (!isAmbientMode || slot.showInAmbient)) {
          val offsetX = (screenPx * slot.offsetXFraction).roundToInt()
          val offsetY = (screenPx * slot.offsetYFraction).roundToInt()
          val modifierAtOffset = Modifier.offset { IntOffset(offsetX, offsetY) }

          val isDial = slot.position == WidgetSlotPosition.LEFT_DIAL ||
            slot.position == WidgetSlotPosition.RIGHT_DIAL

          if (isDial) {
            WatchComplicationDial(
              slot = slot,
              config = config,
              telemetry = telemetry,
              accentColor = accentColor,
              isAmbient = isAmbientMode,
              isSelected = selectedSlotForEdit == slot.position,
              compactScale = compactScale * slot.scale,
              monoFont = monoFontFamily,
              modifier = modifierAtOffset,
              onClick = onSlotClick?.let { cb -> { cb(slot.position) } },
            )
          } else {
            WatchComplicationChip(
              slot = slot,
              config = config,
              telemetry = telemetry,
              accentColor = accentColor,
              isAmbient = isAmbientMode,
              isSelected = selectedSlotForEdit == slot.position,
              compactScale = compactScale * slot.scale,
              monoFont = monoFontFamily,
              modifier = modifierAtOffset,
              onClick = onSlotClick?.let { cb -> { cb(slot.position) } },
            )
          }
        }
      }

      // Optional App Dock Toggle Trigger (positioned cleanly at lower-right rim to never overlap BOTTOM_DOCK)
      if (!isAmbientMode && config.showAppDockButton) {
        Surface(
          onClick = { onToggleAppDock(!showAppDock) },
          shape = CircleShape,
          color = Color(0xCC161E2B),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(x = 58.dp * compactScale, y = (-20).dp * compactScale)
            .size((26.dp * compactScale).coerceAtLeast(22.dp)),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Apps,
              contentDescription = "アプリドックを開く",
              tint = accentColor,
              modifier = Modifier.size(14.dp * compactScale),
            )
          }
        }
      }

      // 5. Notification Frame Edge Glow Layer
      if (isPulseActive && !isAmbientMode) {
        NotificationEdgeGlowOverlay(
          color = Color(config.notificationAlert.edgeGlowColorHex),
          style = config.notificationAlert.edgeGlowStyle,
          thicknessDp = config.notificationAlert.edgeGlowThicknessDp,
          nowMillis = nowMillis,
        )
      }

      // 6. Interactive Wear App Launcher Dock Overlay
      AnimatedVisibility(
        visible = showAppDock && !isAmbientMode,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f, animationSpec = tween(200)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.94f, animationSpec = tween(150)),
      ) {
        WatchAppDockOverlay(
          shortcuts = config.appDockShortcuts.filter { it.enabled },
          accentColor = accentColor,
          compactScale = compactScale,
          onShortcutClick = { shortcut -> onAppShortcutClick?.invoke(shortcut) },
          onClose = { onToggleAppDock(false) },
        )
      }
    }
  }
}

private fun formatWatchDate(
  year: Int,
  month: Int,
  day: Int,
  dayOfWeek: Int,
  format: DateDisplayFormat,
): String? {
  val enDow = when (dayOfWeek) {
    Calendar.SUNDAY -> "SUN"
    Calendar.MONDAY -> "MON"
    Calendar.TUESDAY -> "TUE"
    Calendar.WEDNESDAY -> "WED"
    Calendar.THURSDAY -> "THU"
    Calendar.FRIDAY -> "FRI"
    else -> "SAT"
  }
  val jpDow = when (dayOfWeek) {
    Calendar.SUNDAY -> "日"
    Calendar.MONDAY -> "月"
    Calendar.TUESDAY -> "火"
    Calendar.WEDNESDAY -> "水"
    Calendar.THURSDAY -> "木"
    Calendar.FRIDAY -> "金"
    else -> "土"
  }
  return when (format) {
    DateDisplayFormat.SHORT_EN -> "$month/$day $enDow"
    DateDisplayFormat.JP_DAY -> "${month}月${day}日($jpDow)"
    DateDisplayFormat.ISO_DATE -> "%04d.%02d.%02d".format(year, month, day)
    DateDisplayFormat.HIDDEN -> null
  }
}

@Composable
private fun NotificationEdgeGlowOverlay(
  color: Color,
  style: NotificationEdgeStyle,
  thicknessDp: Float,
  nowMillis: Long,
) {
  val phase = ((nowMillis % 1600L) / 1600f)
  Canvas(modifier = Modifier.fillMaxSize()) {
    val strokePx = thicknessDp.dp.toPx().coerceIn(2f, 24f)
    val inset = strokePx / 2f
    val arcSize = Size(size.width - strokePx, size.height - strokePx)

    when (style) {
      NotificationEdgeStyle.PULSE_RING -> {
        val pulse = 0.35f + 0.65f * ((sin(phase * 2f * PI).toFloat() + 1f) / 2f)
        drawArc(
          color = color.copy(alpha = pulse),
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = strokePx),
        )
        drawArc(
          color = color.copy(alpha = pulse * 0.3f),
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = Offset(inset + strokePx, inset + strokePx),
          size = Size(size.width - strokePx * 3, size.height - strokePx * 3),
          style = Stroke(width = strokePx * 1.4f),
        )
      }

      NotificationEdgeStyle.ORBIT_SWEEP -> {
        val start = phase * 360f
        drawArc(
          color = color.copy(alpha = 0.18f),
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = strokePx * 0.5f),
        )
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(Color.Transparent, color, Color.White, color, Color.Transparent),
          ),
          startAngle = start,
          sweepAngle = 140f,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = strokePx * 1.3f, cap = StrokeCap.Round),
        )
      }

      NotificationEdgeStyle.DOUBLE_FLASH -> {
        val burst = if (phase < 0.25f || (phase in 0.35f..0.60f)) 0.95f else 0.15f
        drawArc(
          color = color.copy(alpha = burst),
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = strokePx * 1.1f),
        )
      }
    }
  }
}

@Composable
private fun ProceduralShaderBackground(
  shaderPreset: ShaderPresetId,
  accentColor: Color,
  secondaryColor: Color,
  nowMillis: Long,
  isAmbient: Boolean,
) {
  val phase = ((nowMillis % 12000L) / 12000f) * (2f * PI.toFloat())
  Canvas(modifier = Modifier.fillMaxSize()) {
    drawRect(Color.Black)
    if (isAmbient || shaderPreset == ShaderPresetId.PURE_OLED_CARBON) {
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0xFF0C1016), Color.Black),
          center = center,
          radius = size.minDimension * 0.55f,
        ),
      )
      return@Canvas
    }

    when (shaderPreset) {
      ShaderPresetId.AURORA_WAVE -> {
        val c1 = Offset(
          x = center.x + cos(phase) * size.width * 0.22f,
          y = center.y + sin(phase * 0.8f) * size.height * 0.18f,
        )
        val c2 = Offset(
          x = center.x - cos(phase * 0.6f) * size.width * 0.24f,
          y = center.y - sin(phase) * size.height * 0.20f,
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.42f), Color.Transparent),
            center = c1,
            radius = size.minDimension * 0.58f,
          ),
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(secondaryColor.copy(alpha = 0.28f), Color.Transparent),
            center = c2,
            radius = size.minDimension * 0.50f,
          ),
        )
      }

      ShaderPresetId.ORBITAL_RADAR -> {
        val maxR = size.minDimension * 0.48f
        for (ring in 1..4) {
          drawCircle(
            color = accentColor.copy(alpha = 0.12f),
            radius = maxR * (ring / 4f),
            style = Stroke(width = 1.5f),
          )
        }
        val sweepAngle = (phase / (2f * PI.toFloat())) * 360f
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(
              Color.Transparent,
              accentColor.copy(alpha = 0.05f),
              accentColor.copy(alpha = 0.35f),
              Color.Transparent,
            ),
          ),
          startAngle = sweepAngle,
          sweepAngle = 70f,
          useCenter = true,
        )
      }

      ShaderPresetId.STARFIELD_DRIFT -> {
        for (i in 0 until 32) {
          val seedAngle = (i * 137.5f) * (PI.toFloat() / 180f)
          val radial = (((i * 29 + (phase * 40f)) % 100f) / 100f) * (size.minDimension * 0.48f)
          val x = center.x + cos(seedAngle) * radial
          val y = center.y + sin(seedAngle) * radial
          val starColor = if (i % 4 == 0) secondaryColor else accentColor
          drawCircle(
            color = starColor.copy(alpha = 0.3f + (radial / size.minDimension) * 0.6f),
            radius = if (i % 5 == 0) 3.2f else 1.8f,
            center = Offset(x, y),
          )
        }
      }

      ShaderPresetId.PURE_OLED_CARBON -> Unit
    }
  }
}

@Composable
private fun AnalogClockLayer(
  hour24: Int,
  minute: Int,
  second: Int,
  milli: Int,
  smoothSweep: Boolean,
  showSeconds: Boolean,
  tickStyle: TickStyle,
  accentColor: Color,
  secondaryColor: Color,
  handColor: Color,
  isAmbient: Boolean,
  scale: Float,
  clockOffset: IntOffset,
) {
  Canvas(
    modifier = Modifier
      .fillMaxSize()
      .offset { clockOffset },
  ) {
    val radius = (size.minDimension * 0.45f) * scale.coerceIn(0.65f, 1.3f)
    if (tickStyle != TickStyle.NONE) {
      val count = when (tickStyle) {
        TickStyle.PRECISION_60 -> 60
        TickStyle.MINIMAL_12 -> 12
        TickStyle.QUARTER_4 -> 4
        TickStyle.NONE -> 0
      }
      val stepDeg = 360f / count.coerceAtLeast(1)
      for (i in 0 until count) {
        val deg = i * stepDeg - 90f
        val rad = deg * (PI / 180.0)
        val isMajor = (tickStyle == TickStyle.PRECISION_60 && i % 5 == 0) ||
          tickStyle != TickStyle.PRECISION_60
        val innerR = radius * if (isMajor) 0.87f else 0.93f
        val outerR = radius * 0.98f
        drawLine(
          color = if (isMajor) accentColor.copy(alpha = if (isAmbient) 0.55f else 0.9f)
          else Color.White.copy(alpha = 0.25f),
          start = Offset(center.x + (cos(rad) * innerR).toFloat(), center.y + (sin(rad) * innerR).toFloat()),
          end = Offset(center.x + (cos(rad) * outerR).toFloat(), center.y + (sin(rad) * outerR).toFloat()),
          strokeWidth = if (isMajor) 3.2f else 1.4f,
          cap = StrokeCap.Round,
        )
      }
    }

    val exactSecond = second + if (smoothSweep) milli / 1000f else 0f
    val exactMinute = minute + exactSecond / 60f
    val exactHour = (hour24 % 12) + exactMinute / 60f

    val hourRad = (exactHour * 30f - 90f) * (PI / 180.0)
    drawLine(
      color = handColor.copy(alpha = if (isAmbient) 0.72f else 0.95f),
      start = center,
      end = Offset(center.x + (cos(hourRad) * radius * 0.50f).toFloat(), center.y + (sin(hourRad) * radius * 0.50f).toFloat()),
      strokeWidth = 8f,
      cap = StrokeCap.Round,
    )

    val minRad = (exactMinute * 6f - 90f) * (PI / 180.0)
    drawLine(
      color = accentColor.copy(alpha = if (isAmbient) 0.75f else 1f),
      start = center,
      end = Offset(center.x + (cos(minRad) * radius * 0.74f).toFloat(), center.y + (sin(minRad) * radius * 0.74f).toFloat()),
      strokeWidth = 5f,
      cap = StrokeCap.Round,
    )

    if (showSeconds) {
      val secRad = (exactSecond * 6f - 90f) * (PI / 180.0)
      drawLine(
        color = secondaryColor,
        start = Offset(center.x - (cos(secRad) * radius * 0.16f).toFloat(), center.y - (sin(secRad) * radius * 0.16f).toFloat()),
        end = Offset(center.x + (cos(secRad) * radius * 0.84f).toFloat(), center.y + (sin(secRad) * radius * 0.84f).toFloat()),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round,
      )
    }

    drawCircle(color = accentColor, radius = 6f, center = center)
    drawCircle(color = Color.Black, radius = 2.5f, center = center)
  }
}

@Composable
private fun OrbitDigitalClockLayer(
  hour24: Int,
  minute: Int,
  second: Int,
  milli: Int,
  dateText: String?,
  use24Hour: Boolean,
  showSeconds: Boolean,
  tickStyle: TickStyle,
  accentColor: Color,
  secondaryColor: Color,
  hourColor: Color,
  isAmbient: Boolean,
  scale: Float,
  fontWeight: FontWeight,
  clockOffset: IntOffset,
  displayFont: FontFamily,
  monoFont: FontFamily,
) {
  val displayHour = if (use24Hour) hour24 else ((hour24 % 12).let { if (it == 0) 12 else it })
  val timeStr = "%02d:%02d".format(displayHour, minute)
  val secProgress = (second + milli / 1000f) / 60f

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val inset = size.minDimension * 0.04f
      val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
      if (tickStyle != TickStyle.NONE) {
        drawArc(
          color = accentColor.copy(alpha = 0.15f),
          startAngle = -90f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = 3f),
        )
      }
      if (showSeconds) {
        drawArc(
          brush = Brush.sweepGradient(listOf(accentColor, secondaryColor, accentColor)),
          startAngle = -90f,
          sweepAngle = secProgress * 360f,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = 4.5f, cap = StrokeCap.Round),
        )
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.offset { clockOffset },
    ) {
      if (dateText != null) {
        Text(
          text = dateText,
          color = accentColor.copy(alpha = if (isAmbient) 0.65f else 0.92f),
          fontFamily = monoFont,
          fontSize = (10.5f * scale).sp,
          fontWeight = FontWeight.Medium,
          letterSpacing = 1.4.sp,
        )
      }
      Text(
        text = timeStr,
        color = hourColor.copy(alpha = if (isAmbient) 0.78f else 0.98f),
        fontFamily = displayFont,
        fontSize = (40 * scale).sp,
        fontWeight = fontWeight,
        letterSpacing = (-1.5).sp,
      )
      if (showSeconds) {
        Text(
          text = "%02d SEC".format(second),
          color = secondaryColor,
          fontFamily = monoFont,
          fontSize = (9.5f * scale).sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 1.2.sp,
        )
      }
    }
  }
}

@Composable
private fun ModularMinimalClockLayer(
  hour24: Int,
  minute: Int,
  dateText: String?,
  use24Hour: Boolean,
  accentColor: Color,
  hourColor: Color,
  isAmbient: Boolean,
  scale: Float,
  fontWeight: FontWeight,
  clockOffset: IntOffset,
  displayFont: FontFamily,
  monoFont: FontFamily,
) {
  val displayHour = if (use24Hour) hour24 else ((hour24 % 12).let { if (it == 0) 12 else it })
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = accentColor.copy(alpha = if (isAmbient) 0.2f else 0.35f),
        radius = size.minDimension * 0.45f,
        style = Stroke(width = 1.5f),
      )
    }
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.offset { clockOffset },
    ) {
      Text(
        text = "%02d".format(displayHour),
        color = hourColor.copy(alpha = if (isAmbient) 0.75f else 0.98f),
        fontFamily = displayFont,
        fontSize = (40 * scale).sp,
        fontWeight = FontWeight.Light,
        lineHeight = (38 * scale).sp,
      )
      Text(
        text = "%02d".format(minute),
        color = accentColor.copy(alpha = if (isAmbient) 0.75f else 1f),
        fontFamily = displayFont,
        fontSize = (40 * scale).sp,
        fontWeight = fontWeight,
        lineHeight = (38 * scale).sp,
      )
      if (dateText != null) {
        Text(
          text = dateText,
          color = Color(0xFF9AA8BE),
          fontFamily = monoFont,
          fontSize = (9.5f * scale).sp,
        )
      }
    }
  }
}

@Composable
private fun DataMatrixClockLayer(
  hour24: Int,
  minute: Int,
  second: Int,
  dateText: String?,
  modelTag: String?,
  use24Hour: Boolean,
  showSeconds: Boolean,
  accentColor: Color,
  secondaryColor: Color,
  hourColor: Color,
  isAmbient: Boolean,
  scale: Float,
  fontWeight: FontWeight,
  clockOffset: IntOffset,
  monoFont: FontFamily,
) {
  val displayHour = if (use24Hour) hour24 else ((hour24 % 12).let { if (it == 0) 12 else it })
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.offset { clockOffset },
    ) {
      if (modelTag != null || dateText != null) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (modelTag != null) {
            Text(
              text = modelTag,
              color = secondaryColor,
              fontFamily = monoFont,
              fontSize = (8.5f * scale).sp,
              fontWeight = FontWeight.Bold,
            )
          }
          if (dateText != null) {
            Text(
              text = dateText,
              color = Color(0xFF9AA8BE),
              fontFamily = monoFont,
              fontSize = (9 * scale).sp,
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
      }
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = "%02d:%02d".format(displayHour, minute),
          color = hourColor.copy(alpha = if (isAmbient) 0.75f else 0.98f),
          fontFamily = monoFont,
          fontSize = (29 * scale).sp,
          fontWeight = fontWeight,
          letterSpacing = (-1).sp,
        )
        if (showSeconds) {
          Text(
            text = ":%02d".format(second),
            color = accentColor,
            fontFamily = monoFont,
            fontSize = (15 * scale).sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 3.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun WatchComplicationChip(
  slot: WidgetSlotConfig,
  config: WatchHomeConfig,
  telemetry: WatchTelemetrySnapshot,
  accentColor: Color,
  isAmbient: Boolean,
  isSelected: Boolean,
  compactScale: Float,
  monoFont: FontFamily,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)?,
) {
  val plugin = remember(slot.pluginId, config.customExtensions) {
    WatchExtensionRegistry.findPlugin(slot.pluginId, config.customExtensions)
  }
  val resolved = remember(plugin, slot, telemetry) {
    plugin.resolveDisplay(slot, telemetry)
  }
  val slotAccent = slot.accentOverrideHex?.let { Color(it) } ?: accentColor
  val bgAlpha = if (isAmbient) 0.85f else slot.backgroundAlpha.coerceIn(0f, 0.95f)

  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(50))
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    shape = RoundedCornerShape(50),
    color = if (isAmbient) Color(0xFF0A0D12).copy(alpha = bgAlpha) else Color(0xFF141B26).copy(alpha = bgAlpha),
    border = if (bgAlpha > 0.08f || isSelected) {
      BorderStroke(
        width = if (isSelected) 1.5.dp else 0.8.dp,
        color = if (isSelected) SharedWatchPalette.AmberAccent else slotAccent.copy(alpha = 0.45f),
      )
    } else null,
  ) {
    Row(
      modifier = Modifier.padding(
        horizontal = (8.dp * compactScale).coerceAtLeast(5.dp),
        vertical = (4.dp * compactScale).coerceAtLeast(2.dp),
      ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp * compactScale),
    ) {
      Icon(
        imageVector = resolvePluginIcon(resolved.iconKey),
        contentDescription = plugin.displayName,
        tint = slotAccent,
        modifier = Modifier.size((12.dp * compactScale).coerceAtLeast(9.dp)),
      )
      Text(
        text = "${resolved.title} ${resolved.primaryValue}".trim(),
        color = Color.White.copy(alpha = if (isAmbient) 0.72f else 0.95f),
        fontFamily = monoFont,
        fontSize = (9.2f * compactScale).sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun WatchComplicationDial(
  slot: WidgetSlotConfig,
  config: WatchHomeConfig,
  telemetry: WatchTelemetrySnapshot,
  accentColor: Color,
  isAmbient: Boolean,
  isSelected: Boolean,
  compactScale: Float,
  monoFont: FontFamily,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)?,
) {
  val plugin = remember(slot.pluginId, config.customExtensions) {
    WatchExtensionRegistry.findPlugin(slot.pluginId, config.customExtensions)
  }
  val resolved = remember(plugin, slot, telemetry) {
    plugin.resolveDisplay(slot, telemetry)
  }
  val slotAccent = slot.accentOverrideHex?.let { Color(it) } ?: accentColor
  val dialSize = (42.dp * compactScale).coerceAtLeast(32.dp)
  val bgAlpha = if (isAmbient) 0.85f else slot.backgroundAlpha.coerceIn(0f, 0.95f)

  Box(
    modifier = modifier
      .size(dialSize)
      .clip(CircleShape)
      .background(
        if (isAmbient) Color(0xFF0A0D12).copy(alpha = bgAlpha)
        else Color(0xFF141B26).copy(alpha = bgAlpha),
      )
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    if (bgAlpha > 0.08f || isSelected || resolved.progress != null) {
      Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
        if (bgAlpha > 0.08f || isSelected) {
          drawCircle(
            color = if (isSelected) SharedWatchPalette.AmberAccent else slotAccent.copy(alpha = 0.25f),
            style = Stroke(width = if (isSelected) 3f else 1.8f),
          )
        }
        resolved.progress?.let { pct ->
          drawArc(
            color = slotAccent,
            startAngle = -90f,
            sweepAngle = pct * 360f,
            useCenter = false,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
          )
        }
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = resolvePluginIcon(resolved.iconKey),
        contentDescription = plugin.displayName,
        tint = slotAccent,
        modifier = Modifier.size((11.dp * compactScale).coerceAtLeast(8.dp)),
      )
      Text(
        text = resolved.primaryValue,
        color = Color.White.copy(alpha = if (isAmbient) 0.72f else 0.96f),
        fontFamily = monoFont,
        fontSize = (8.2f * compactScale).sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun WatchAppDockOverlay(
  shortcuts: List<AppDockShortcut>,
  accentColor: Color,
  compactScale: Float,
  onShortcutClick: (AppDockShortcut) -> Unit,
  onClose: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xEB070A10))
      .padding((18.dp * compactScale).coerceAtLeast(12.dp)),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Spacer(modifier = Modifier.width(24.dp))
        Text(
          text = "APPS",
          color = accentColor,
          fontSize = (10 * compactScale).sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp,
        )
        Surface(
          onClick = onClose,
          shape = CircleShape,
          color = Color(0xFF1D2636),
          modifier = Modifier.size((24.dp * compactScale).coerceAtLeast(22.dp)),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "ドックを閉じる",
              tint = Color.White,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp * compactScale))

      LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp * compactScale),
        horizontalArrangement = Arrangement.spacedBy(6.dp * compactScale),
        modifier = Modifier.weight(1f),
      ) {
        items(shortcuts, key = { it.id }) { item ->
          Column(
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .clickable { onShortcutClick(item) }
              .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Box(
              modifier = Modifier
                .size((34.dp * compactScale).coerceAtLeast(28.dp))
                .clip(CircleShape)
                .background(Color(0xFF182232))
                .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = resolvePluginIcon(item.iconKey),
                contentDescription = item.label,
                tint = accentColor,
                modifier = Modifier.size((17.dp * compactScale).coerceAtLeast(14.dp)),
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = item.label,
              color = Color.White,
              fontSize = (8 * compactScale).sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
