package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.WatchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.chronocanvas.shared.model.WatchModelSpec
import com.aistudio.chronocanvas.shared.model.WidgetSlotPosition
import com.aistudio.chronocanvas.shared.ui.SharedWatchHomeCanvas
import com.example.media.PhonePresetArtworkSlot
import com.example.media.PhoneVideoWallpaperSurface
import com.example.ui.studio.StudioUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveWatchStage(
  uiState: StudioUiState,
  onSelectModel: (WatchModelSpec) -> Unit,
  onToggleAmbient: (Boolean) -> Unit,
  onToggleAppDock: (Boolean) -> Unit,
  onOpenFullscreen: () -> Unit,
  onSlotClick: ((WidgetSlotPosition) -> Unit)? = null,
  onAppShortcutMessage: (String) -> Unit = {},
  onToggleOffWrist: ((Boolean) -> Unit)? = null,
  onTestNotification: (() -> Unit)? = null,
  onSelectPreviewHour: ((Int?) -> Unit)? = null,
  watchDiameter: Dp = 264.dp,
  showControls: Boolean = true,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              MaterialTheme.colorScheme.surfaceContainer,
              MaterialTheme.colorScheme.surface,
            ),
          ),
        )
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = if (uiState.telemetry.isOffWrist) Icons.Rounded.WatchOff else Icons.Rounded.Watch,
            contentDescription = null,
            tint = if (uiState.telemetry.isOffWrist) {
              MaterialTheme.colorScheme.secondary
            } else {
              MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = uiState.activeConfig.watchModel.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.BatterySaver,
                contentDescription = "推定駆動時間",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = "${"%.0f".format(uiState.powerReport.estimatedHours)}h",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          IconButton(
            onClick = onOpenFullscreen,
            modifier = Modifier
              .size(48.dp)
              .testTag("fullscreen_watch_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Fullscreen,
              contentDescription = "ウォッチ画面を拡大表示",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        SharedWatchHomeCanvas(
          config = uiState.activeConfig,
          telemetry = uiState.telemetry,
          isAmbientMode = uiState.isAmbientPreview,
          showAppDock = uiState.showAppDockPreview,
          onToggleAppDock = onToggleAppDock,
          selectedSlotForEdit = uiState.selectedSlotForEdit,
          onSlotClick = onSlotClick,
          onAppShortcutClick = { shortcut ->
            onAppShortcutMessage("ウォッチランチャー起動: ${shortcut.label} (${shortcut.packageHint})")
            onToggleAppDock(false)
          },
          previewHourOverride = uiState.previewHourOverride,
          displayFontFamily = SpaceGroteskFamily,
          monoFontFamily = JetBrainsMonoFamily,
          videoWallpaperSlot = { wp, ambient ->
            PhoneVideoWallpaperSurface(wallpaper = wp, isAmbient = ambient)
          },
          presetArtworkSlot = { artId, ambient ->
            PhonePresetArtworkSlot(presetArtId = artId, isAmbient = ambient)
          },
          modifier = Modifier
            .widthIn(max = watchDiameter)
            .fillMaxWidth(0.82f)
            .testTag("watch_live_canvas"),
        )
      }

      if (showControls) {
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterChip(
            selected = uiState.activeConfig.watchModel == WatchModelSpec.PIXEL_WATCH_3_45MM,
            onClick = { onSelectModel(WatchModelSpec.PIXEL_WATCH_3_45MM) },
            label = { Text("45mm") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.testTag("model_45mm_chip"),
          )
          FilterChip(
            selected = uiState.activeConfig.watchModel == WatchModelSpec.PIXEL_WATCH_3_41MM,
            onClick = { onSelectModel(WatchModelSpec.PIXEL_WATCH_3_41MM) },
            label = { Text("41mm") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.testTag("model_41mm_chip"),
          )
          FilterChip(
            selected = uiState.isAmbientPreview,
            onClick = { onToggleAmbient(!uiState.isAmbientPreview) },
            leadingIcon = {
              Icon(
                imageVector = if (uiState.isAmbientPreview) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text(if (uiState.isAmbientPreview) "AOD 1Hz" else "通常表示") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.secondary,
            ),
            modifier = Modifier.testTag("toggle_ambient_chip"),
          )
          FilterChip(
            selected = uiState.showAppDockPreview,
            onClick = { onToggleAppDock(!uiState.showAppDockPreview) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text("ドック") },
            modifier = Modifier.testTag("toggle_dock_chip"),
          )
          if (onToggleOffWrist != null) {
            FilterChip(
              selected = uiState.telemetry.isOffWrist,
              onClick = { onToggleOffWrist(!uiState.telemetry.isOffWrist) },
              leadingIcon = {
                Icon(
                  imageVector = if (uiState.telemetry.isOffWrist) Icons.Rounded.WatchOff else Icons.Rounded.Watch,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text(if (uiState.telemetry.isOffWrist) "外した状態" else "装着中") },
              modifier = Modifier.testTag("toggle_offwrist_chip"),
            )
          }
          if (onTestNotification != null) {
            FilterChip(
              selected = uiState.telemetry.unreadNotificationCount > 0,
              onClick = onTestNotification,
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.NotificationsActive,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = {
                Text(
                  if (uiState.telemetry.unreadNotificationCount > 0) {
                    "通知${uiState.telemetry.unreadNotificationCount}件"
                  } else {
                    "通知テスト"
                  },
                )
              },
              modifier = Modifier.testTag("test_notification_chip"),
            )
          }
          if (onSelectPreviewHour != null) {
            val nextHourLabel = when (uiState.previewHourOverride) {
              null -> "実時刻"
              9 -> "朝 09時"
              15 -> "昼 15時"
              20 -> "夜 20時"
              else -> "深夜 02時"
            }
            FilterChip(
              selected = uiState.previewHourOverride != null,
              onClick = {
                val next = when (uiState.previewHourOverride) {
                  null -> 9
                  9 -> 15
                  15 -> 20
                  20 -> 2
                  else -> null
                }
                onSelectPreviewHour(next)
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.Schedule,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text(nextHourLabel) },
              modifier = Modifier.testTag("cycle_preview_hour_chip"),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullscreenWatchSimulatorDialog(
  uiState: StudioUiState,
  onDismiss: () -> Unit,
  onToggleAmbient: (Boolean) -> Unit,
  onToggleAppDock: (Boolean) -> Unit,
  onAppShortcutMessage: (String) -> Unit,
  onToggleOffWrist: ((Boolean) -> Unit)? = null,
  onTestNotification: (() -> Unit)? = null,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    BackHandler(onBack = onDismiss)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xF5040609))
        .padding(20.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Pixel Watch 3 シミュレータ",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
            Text(
              text = "${uiState.activeConfig.watchModel.nativePx}×${uiState.activeConfig.watchModel.nativePx}px · 推定 ${"%.1f".format(uiState.powerReport.estimatedHours)}時間",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Surface(
            onClick = onDismiss,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
              .size(48.dp)
              .testTag("close_fullscreen_watch"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "拡大表示を閉じる",
                tint = Color.White,
              )
            }
          }
        }

        SharedWatchHomeCanvas(
          config = uiState.activeConfig,
          telemetry = uiState.telemetry,
          isAmbientMode = uiState.isAmbientPreview,
          showAppDock = uiState.showAppDockPreview,
          onToggleAppDock = onToggleAppDock,
          onAppShortcutClick = { shortcut ->
            onAppShortcutMessage("ウォッチランチャー起動: ${shortcut.label}")
            onToggleAppDock(false)
          },
          previewHourOverride = uiState.previewHourOverride,
          displayFontFamily = SpaceGroteskFamily,
          monoFontFamily = JetBrainsMonoFamily,
          videoWallpaperSlot = { wp, ambient ->
            PhoneVideoWallpaperSurface(wallpaper = wp, isAmbient = ambient)
          },
          presetArtworkSlot = { artId, ambient ->
            PhonePresetArtworkSlot(presetArtId = artId, isAmbient = ambient)
          },
          modifier = Modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth(0.9f),
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = uiState.isAmbientPreview,
            onClick = { onToggleAmbient(!uiState.isAmbientPreview) },
            label = {
              Text(
                if (uiState.isAmbientPreview) "AOD (1Hz省電力中)" else "通常アクティブ",
                fontWeight = FontWeight.SemiBold,
              )
            },
          )
          FilterChip(
            selected = uiState.showAppDockPreview,
            onClick = { onToggleAppDock(!uiState.showAppDockPreview) },
            label = { Text("アプリドック") },
          )
          if (onToggleOffWrist != null) {
            FilterChip(
              selected = uiState.telemetry.isOffWrist,
              onClick = { onToggleOffWrist(!uiState.telemetry.isOffWrist) },
              label = { Text(if (uiState.telemetry.isOffWrist) "外した状態" else "装着中") },
            )
          }
          if (onTestNotification != null) {
            FilterChip(
              selected = uiState.telemetry.unreadNotificationCount > 0,
              onClick = onTestNotification,
              label = { Text("通知発光 (${uiState.telemetry.unreadNotificationCount})") },
            )
          }
        }
      }
    }
  }
}
