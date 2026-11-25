package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.chronocanvas.shared.model.WatchModelSpec
import com.aistudio.chronocanvas.shared.model.WidgetSlotPosition
import com.example.R
import com.example.data.local.WatchPresetEntity
import com.example.ui.components.InteractiveWatchStage
import com.example.ui.studio.StudioTab
import com.example.ui.studio.StudioUiState

@Composable
fun StudioHomeScreen(
  uiState: StudioUiState,
  onSelectModel: (WatchModelSpec) -> Unit,
  onToggleAmbient: (Boolean) -> Unit,
  onToggleAppDock: (Boolean) -> Unit,
  onOpenFullscreen: () -> Unit,
  onSlotTappedToEdit: (WidgetSlotPosition) -> Unit,
  onLoadPreset: (WatchPresetEntity) -> Unit,
  onSavePreset: (String) -> Unit,
  onDeletePreset: (String) -> Unit,
  onSyncNow: () -> Unit,
  onNavigateTab: (StudioTab) -> Unit,
  onShowMessage: (String) -> Unit,
  onToggleOffWrist: (Boolean) -> Unit = {},
  onTestNotification: () -> Unit = {},
  onSelectPreviewHour: (Int?) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showSaveDialog by remember { mutableStateOf(false) }
  var presetNameInput by remember { mutableStateOf("") }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Studio Hero Banner
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_hero_studio_1795588543199),
            contentDescription = "ChronoCanvas Studio バナー",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  colors = listOf(
                    Color(0xEE07090E),
                    Color(0x9907090E),
                    Color(0x3307090E),
                  ),
                ),
              ),
          )
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
            verticalArrangement = Arrangement.Center,
          ) {
            Text(
              text = "CHRONOCANVAS STUDIO",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Pixel Watch 3 拡張ホーム",
              style = MaterialTheme.typography.headlineSmall,
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "時計・ウィジェット・動画壁紙・省電力制御をリアルタイム同期",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFD0DAE8),
            )
          }
        }
      }
    }

    // 2. Live Interactive Pixel Watch 3 Stage
    item {
      InteractiveWatchStage(
        uiState = uiState,
        onSelectModel = onSelectModel,
        onToggleAmbient = onToggleAmbient,
        onToggleAppDock = onToggleAppDock,
        onOpenFullscreen = onOpenFullscreen,
        onSlotClick = { pos ->
          onSlotTappedToEdit(pos)
          onNavigateTab(StudioTab.CLOCK_WIDGETS)
        },
        onAppShortcutMessage = onShowMessage,
        onToggleOffWrist = onToggleOffWrist,
        onTestNotification = onTestNotification,
        onSelectPreviewHour = onSelectPreviewHour,
      )
    }

    // 3. Wear OS Sync CTA Bar
    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Watch,
                contentDescription = null,
                tint = if (uiState.connectedNodes.isNotEmpty()) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = if (uiState.connectedNodes.isNotEmpty()) {
                  "接続中: ${uiState.connectedNodes.first().displayName}"
                } else {
                  "Watch未接続 (ローカル検証モード)"
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            val otaLabel = if (uiState.autoSyncEnabled) "OTA自動反映 ON" else "手動同期"
            val watchVerLabel = uiState.watchAppStatus?.let { " · Watch v${it.versionName}" }.orEmpty()
            Text(
              text = "駆動 ${"%.1f".format(uiState.powerReport.estimatedHours)}h · $otaLabel$watchVerLabel",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = onSyncNow,
            enabled = !uiState.isSyncing,
            modifier = Modifier.testTag("studio_sync_watch_button"),
          ) {
            if (uiState.isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("同期送信")
          }
        }
      }
    }

    // 4. Quick Feature Shortcuts
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        QuickShortcutTile(
          icon = Icons.Rounded.AutoFixHigh,
          title = "時計・拡張",
          subtitle = uiState.activeConfig.clockStyle.styleId.title,
          onClick = { onNavigateTab(StudioTab.CLOCK_WIDGETS) },
          modifier = Modifier
            .weight(1f)
            .testTag("shortcut_clock_widgets"),
        )
        QuickShortcutTile(
          icon = Icons.Rounded.Movie,
          title = "動画壁紙",
          subtitle = uiState.activeConfig.wallpaper.sourceType.label,
          onClick = { onNavigateTab(StudioTab.WALLPAPER) },
          modifier = Modifier
            .weight(1f)
            .testTag("shortcut_wallpaper"),
        )
        QuickShortcutTile(
          icon = Icons.Rounded.BatteryChargingFull,
          title = "省電力",
          subtitle = "${uiState.powerReport.efficiencyScore}点",
          onClick = { onNavigateTab(StudioTab.BATTERY_SYNC) },
          modifier = Modifier
            .weight(1f)
            .testTag("shortcut_battery"),
        )
      }
    }

    // 5. Saved Presets Carousel
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "プリセット & カスタム構成",
          style = MaterialTheme.typography.titleLarge,
        )
        OutlinedButton(
          onClick = {
            presetNameInput = ""
            showSaveDialog = true
          },
          modifier = Modifier.testTag("save_preset_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("現在を保存")
        }
      }
    }

    item {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(uiState.savedPresets, key = { it.id }) { preset ->
          val isActive = uiState.activeConfig.id == preset.id
          Surface(
            modifier = Modifier
              .width(210.dp)
              .clip(MaterialTheme.shapes.medium)
              .clickable { onLoadPreset(preset) }
              .testTag("preset_card_${preset.id}"),
            shape = MaterialTheme.shapes.medium,
            color = if (isActive) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
              MaterialTheme.colorScheme.surfaceContainer
            },
            border = BorderStroke(
              width = if (isActive) 1.5.dp else 1.dp,
              color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (preset.isBuiltIn) "BUILT-IN" else "CUSTOM",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (preset.isBuiltIn) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.secondary
                  },
                )
                if (!preset.isBuiltIn) {
                  IconButton(
                    onClick = { onDeletePreset(preset.id) },
                    modifier = Modifier.size(28.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.DeleteOutline,
                      contentDescription = "プリセットを削除",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = preset.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }

  if (showSaveDialog) {
    AlertDialog(
      onDismissRequest = { showSaveDialog = false },
      title = { Text("カスタム構成を保存") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "現在の時計スタイル・ウィジェット・動画壁紙・省電力設定をローカルDBへ保存します。",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = presetNameInput,
            onValueChange = { presetNameInput = it },
            label = { Text("プリセット名") },
            placeholder = { Text("例: ワークアウト動画フェイス") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("preset_name_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSavePreset(presetNameInput)
            showSaveDialog = false
          },
          colors = ButtonDefaults.buttonColors(),
          modifier = Modifier.testTag("confirm_save_preset"),
        ) {
          Text("保存する")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSaveDialog = false }) {
          Text("キャンセル")
        }
      },
    )
  }
}

@Composable
private fun QuickShortcutTile(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
