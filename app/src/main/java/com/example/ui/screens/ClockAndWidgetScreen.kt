package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.chronocanvas.shared.model.ClockFontWeightLevel
import com.aistudio.chronocanvas.shared.model.ClockStyleId
import com.aistudio.chronocanvas.shared.model.DateDisplayFormat
import com.aistudio.chronocanvas.shared.model.NotificationEdgeStyle
import com.aistudio.chronocanvas.shared.model.TickStyle
import com.aistudio.chronocanvas.shared.model.WidgetSlotPosition
import com.aistudio.chronocanvas.shared.registry.PLUGIN_NONE
import com.aistudio.chronocanvas.shared.registry.WatchExtensionRegistry
import com.aistudio.chronocanvas.shared.ui.SharedWatchPalette
import com.aistudio.chronocanvas.shared.ui.resolvePluginIcon
import com.example.ui.components.InteractiveWatchStage
import com.example.ui.studio.StudioUiState
import com.example.ui.studio.StudioViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClockAndWidgetScreen(
  uiState: StudioUiState,
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showCustomExtDialog by remember { mutableStateOf(false) }

  val activeSlot = remember(uiState.activeConfig.widgetSlots, uiState.selectedSlotForEdit) {
    uiState.activeConfig.widgetSlots.firstOrNull { it.position == uiState.selectedSlotForEdit }
      ?: uiState.activeConfig.widgetSlots.first()
  }
  val allPlugins = remember(uiState.activeConfig.customExtensions) {
    WatchExtensionRegistry.getAllPlugins(uiState.activeConfig.customExtensions)
  }

  var customTitleInput by remember(activeSlot.position, activeSlot.customTitle) {
    mutableStateOf(activeSlot.customTitle.orEmpty())
  }
  var customValueInput by remember(activeSlot.position, activeSlot.customValue) {
    mutableStateOf(activeSlot.customValue.orEmpty())
  }

  val clock = uiState.activeConfig.clockStyle
  val alertCfg = uiState.activeConfig.notificationAlert

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Live Watch Preview (tap any widget directly on the watch!)
    item {
      InteractiveWatchStage(
        uiState = uiState,
        onSelectModel = viewModel::setWatchModel,
        onToggleAmbient = viewModel::toggleAmbientPreview,
        onToggleAppDock = viewModel::toggleAppDockPreview,
        onOpenFullscreen = { viewModel.setFullscreenWatchMode(true) },
        onSlotClick = { viewModel.selectSlotForEdit(it) },
        onAppShortcutMessage = viewModel::showMessage,
        onToggleOffWrist = viewModel::toggleSimOffWrist,
        onTestNotification = viewModel::triggerTestNotificationPulse,
        onSelectPreviewHour = viewModel::setPreviewHourOverride,
        watchDiameter = 224.dp,
        showControls = true,
      )
    }

    // 1. Clock Style Engine Selector
    item {
      Text(
        text = "時計スタイルエンジン",
        style = MaterialTheme.typography.titleLarge,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ClockStyleId.entries.forEach { style ->
          val selected = clock.styleId == style
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .clickable { viewModel.setClockStyle(style) }
              .testTag("clock_style_${style.name}"),
            shape = MaterialTheme.shapes.medium,
            color = if (selected) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
              MaterialTheme.colorScheme.surfaceContainer
            },
            border = BorderStroke(
              width = if (selected) 1.5.dp else 1.dp,
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(text = style.title, style = MaterialTheme.typography.titleMedium)
                Text(
                  text = style.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (selected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = "選択中",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      }
    }

    // 2. Clock Fine Positioning, Colors, Font Weight, Date & Opacity
    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("時計の位置・フォント・カラー詳細設定", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
              onClick = viewModel::resetClockPosition,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.testTag("reset_clock_pos_button"),
            ) {
              Icon(Icons.Rounded.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("中央に戻す")
            }
          }

          Text(
            text = "アクセントカラー（針・リング・強調）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          ColorSwatchRow(
            selectedHex = clock.accentColorHex,
            onSelect = viewModel::setClockAccent,
            tagPrefix = "accent_swatch",
          )

          Text(
            text = "時分テキスト / 時針カラー",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          ColorSwatchRow(
            selectedHex = clock.hourTextColorHex,
            onSelect = viewModel::setClockHourColor,
            tagPrefix = "hour_swatch",
          )

          Text(
            text = "日付（何月何日）文字カラー",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          ColorSwatchRow(
            selectedHex = clock.dateTextColorHex,
            onSelect = viewModel::setClockDateColor,
            tagPrefix = "date_swatch",
          )

          Text("フォントの太さ", style = MaterialTheme.typography.bodySmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClockFontWeightLevel.entries.forEach { fw ->
              FilterChip(
                selected = clock.fontWeight == fw,
                onClick = { viewModel.setClockFontWeight(fw) },
                label = { Text(fw.label) },
              )
            }
          }

          Text("日付表示フォーマット", style = MaterialTheme.typography.bodySmall)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DateDisplayFormat.entries.forEach { fmt ->
              FilterChip(
                selected = clock.dateFormat == fmt,
                onClick = { viewModel.setDateFormat(fmt) },
                label = { Text(fmt.label) },
              )
            }
          }

          LabeledSlider(
            label = "時計サイズ (スケール)",
            valueText = "${"%.2f".format(clock.clockScale)}x",
            value = clock.clockScale,
            onValueChange = viewModel::setClockScale,
            valueRange = 0.65f..1.35f,
            testTag = "clock_scale_slider",
          )

          LabeledSlider(
            label = "時計の不透明度（背景の動画を見せたい時に調整）",
            valueText = "${(clock.clockOpacity * 100).toInt()}%",
            value = clock.clockOpacity,
            onValueChange = viewModel::setClockOpacity,
            valueRange = 0.25f..1.0f,
            testTag = "clock_opacity_slider",
          )

          LabeledSlider(
            label = "時計 X 位置（左右移動）",
            valueText = "${(clock.offsetXFraction * 100).toInt()}%",
            value = clock.offsetXFraction,
            onValueChange = { viewModel.setClockOffset(offsetX = it) },
            valueRange = -0.30f..0.30f,
            testTag = "clock_offset_x_slider",
          )

          LabeledSlider(
            label = "時計 Y 位置（上下移動）",
            valueText = "${(clock.offsetYFraction * 100).toInt()}%",
            value = clock.offsetYFraction,
            onValueChange = { viewModel.setClockOffset(offsetY = it) },
            valueRange = -0.32f..0.32f,
            testTag = "clock_offset_y_slider",
          )

          Text("目盛り / リングスタイル", style = MaterialTheme.typography.bodySmall)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            TickStyle.entries.forEach { tick ->
              FilterChip(
                selected = clock.tickStyle == tick,
                onClick = { viewModel.setTickStyle(tick) },
                label = { Text(tick.label) },
              )
            }
          }

          ToggleOptionRow(
            title = "24時間表示",
            subtitle = "13:00 / 24時間表記を使用",
            checked = clock.use24Hour,
            onCheckedChange = viewModel::setUse24Hour,
          )
          ToggleOptionRow(
            title = "秒針 / 秒リング表示",
            subtitle = "アクティブ時の秒表示（AOD時は自動停止）",
            checked = clock.showSeconds,
            onCheckedChange = viewModel::setShowSeconds,
          )
          ToggleOptionRow(
            title = "スイープ秒針アニメーション",
            subtitle = "OFFにすると毎秒ステップになり省電力化",
            checked = clock.smoothSweepSeconds,
            onCheckedChange = viewModel::setSmoothSweep,
          )
          ToggleOptionRow(
            title = "AODモノクロ省電力モード",
            subtitle = "常時表示時にグレースケール化してOLED劣化と消費を抑制",
            checked = clock.ambientMonochrome,
            onCheckedChange = viewModel::setAmbientMonochrome,
          )
        }
      }
    }

    // 3. Notification Count Bridge & Edge Frame Glow Card
    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "通知カウント連動 & フレーム発光アラート",
              style = MaterialTheme.typography.titleMedium,
            )
          }

          Surface(
            shape = MaterialTheme.shapes.small,
            color = if (uiState.notificationAccessGranted) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
              MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = when {
                    uiState.notificationAccessGranted && uiState.notificationListenerConnected ->
                      "OS通知リスナー稼働中（未読 ${uiState.telemetry.unreadNotificationCount}件）"
                    uiState.notificationAccessGranted ->
                      "通知アクセス許可済み・サービス再接続待ち（未読 ${uiState.telemetry.unreadNotificationCount}件）"
                    else ->
                      "未読件数の自動取得には「通知へのアクセス」許可が必要です"
                  },
                  style = MaterialTheme.typography.labelLarge,
                )
                Text(
                  text = if (uiState.notificationAccessGranted) {
                    "アプリを閉じていても通知変化とスマホ電池(${uiState.telemetry.phoneBatteryPct}%)をWatchへ自動送信します。"
                  } else {
                    "サイドロード版で設定が押せない時は「設定＞アプリ＞ChronoCanvas＞右上⋮＞制限付き設定を許可」を先にONにしてください。"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              if (!uiState.notificationAccessGranted) {
                OutlinedButton(
                  onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    runCatching { context.startActivity(intent) }.onFailure {
                      viewModel.showMessage("設定アプリから「通知へのアクセス」を開いてください")
                    }
                  },
                  modifier = Modifier.testTag("open_notification_access_settings"),
                ) {
                  Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("設定")
                }
              } else {
                OutlinedButton(
                  onClick = {
                    viewModel.refreshNotificationPermissionStatus()
                    viewModel.syncLiveTelemetryNow()
                    viewModel.showMessage("通知・スマホ電池(${uiState.telemetry.phoneBatteryPct}%)をWatchへ再同期しました")
                  },
                  modifier = Modifier.testTag("resync_notification_telemetry_button"),
                ) {
                  Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("再同期")
                }
              }
            }
          }

          ToggleOptionRow(
            title = "通知受信時にウォッチ外周フレームを光らせる",
            subtitle = "円形ベゼルの縁を光らせて視認性高く通知を知らせます",
            checked = alertCfg.edgeGlowEnabled,
            onCheckedChange = { viewModel.updateNotificationAlert(enabled = it) },
          )

          if (alertCfg.edgeGlowEnabled) {
            Text(
              text = "フレーム発光カラー",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ColorSwatchRow(
              selectedHex = alertCfg.edgeGlowColorHex,
              onSelect = { viewModel.updateNotificationAlert(colorHex = it) },
              tagPrefix = "glow_swatch",
            )

            Text("発光アニメーションパターン", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              NotificationEdgeStyle.entries.forEach { st ->
                FilterChip(
                  selected = alertCfg.edgeGlowStyle == st,
                  onClick = { viewModel.updateNotificationAlert(style = st) },
                  label = { Text(st.label) },
                )
              }
            }

            LabeledSlider(
              label = "フレームの太さ",
              valueText = "${"%.1f".format(alertCfg.edgeGlowThicknessDp)} dp",
              value = alertCfg.edgeGlowThicknessDp,
              onValueChange = { viewModel.updateNotificationAlert(thicknessDp = it) },
              valueRange = 2f..10f,
            )

            ToggleOptionRow(
              title = "未読通知がある間ずっとフレームを光らせる",
              subtitle = "OFFにすると受信直後の指定秒数だけ発光します",
              checked = alertCfg.glowWhileUnread,
              onCheckedChange = { viewModel.updateNotificationAlert(glowWhileUnread = it) },
            )

            Text("新着フラッシュ維持時間", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(3, 6, 12).forEach { sec ->
                FilterChip(
                  selected = alertCfg.flashDurationSec == sec,
                  onClick = { viewModel.updateNotificationAlert(flashDurationSec = sec) },
                  label = { Text("${sec}秒") },
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = viewModel::triggerTestNotificationPulse,
              modifier = Modifier
                .weight(1f)
                .testTag("trigger_notification_test_btn"),
            ) {
              Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("通知テスト (+1件)")
            }
            OutlinedButton(
              onClick = viewModel::clearNotificationsPreview,
              modifier = Modifier.testTag("clear_notifications_btn"),
            ) {
              Text("0件に戻す")
            }
          }
        }
      }
    }

    // 4. Modular Widget / Complication Slots + Extensibility Builder
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "ウィジェット & 自由配置スロット",
          style = MaterialTheme.typography.titleLarge,
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OutlinedButton(
            onClick = viewModel::resetAllPositionsToNonOverlappingDefaults,
            modifier = Modifier.testTag("reset_non_overlapping_button"),
          ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("重なり防止配置")
          }
          OutlinedButton(
            onClick = viewModel::clearAllWidgets,
            modifier = Modifier.testTag("clear_all_widgets_button"),
          ) {
            Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("すべて非表示")
          }
          OutlinedButton(
            onClick = { showCustomExtDialog = true },
            modifier = Modifier.testTag("open_custom_extension_dialog"),
          ) {
            Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("拡張作成")
          }
        }

        // Slot Selector Pills
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          WidgetSlotPosition.entries.forEach { pos ->
            FilterChip(
              selected = uiState.selectedSlotForEdit == pos,
              onClick = { viewModel.selectSlotForEdit(pos) },
              label = { Text(pos.shortLabel) },
              modifier = Modifier
                .weight(1f)
                .testTag("slot_tab_${pos.name}"),
            )
          }
        }
      }
    }

    // Selected Slot Configuration Card
    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "${activeSlot.position.label} の設定",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            allPlugins.forEach { plugin ->
              val isAssigned = activeSlot.pluginId == plugin.id
              FilterChip(
                selected = isAssigned,
                onClick = { viewModel.assignPluginToSlot(activeSlot.position, plugin.id) },
                leadingIcon = {
                  Icon(
                    imageVector = resolvePluginIcon(plugin.iconKey),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                },
                label = {
                  Text(
                    text = if (plugin.isBuiltIn) plugin.displayName else "★ ${plugin.displayName}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                modifier = Modifier.testTag("assign_plugin_${plugin.id}"),
              )
            }
          }

          if (activeSlot.pluginId == PLUGIN_NONE) {
            Text(
              text = "このスロットは「なし（非表示）」に設定されています。背景の動画や時計をすっきり見せることができます。",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            LabeledSlider(
              label = "X 位置（左右移動）",
              valueText = "${(activeSlot.offsetXFraction * 100).toInt()}%",
              value = activeSlot.offsetXFraction,
              onValueChange = { viewModel.updateSlotLayout(activeSlot.position, offsetX = it) },
              valueRange = -0.38f..0.38f,
              testTag = "slot_offset_x_slider",
            )

            LabeledSlider(
              label = "Y 位置（上下移動）",
              valueText = "${(activeSlot.offsetYFraction * 100).toInt()}%",
              value = activeSlot.offsetYFraction,
              onValueChange = { viewModel.updateSlotLayout(activeSlot.position, offsetY = it) },
              valueRange = -0.40f..0.40f,
              testTag = "slot_offset_y_slider",
            )

            LabeledSlider(
              label = "ウィジェットサイズ",
              valueText = "${"%.2f".format(activeSlot.scale)}x",
              value = activeSlot.scale,
              onValueChange = { viewModel.updateSlotLayout(activeSlot.position, scale = it) },
              valueRange = 0.65f..1.35f,
              testTag = "slot_scale_slider",
            )

            LabeledSlider(
              label = "背景プレートの濃さ（0%で完全透明）",
              valueText = "${(activeSlot.backgroundAlpha * 100).toInt()}%",
              value = activeSlot.backgroundAlpha,
              onValueChange = { viewModel.updateSlotLayout(activeSlot.position, backgroundAlpha = it) },
              valueRange = 0f..0.95f,
              testTag = "slot_bg_alpha_slider",
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                value = customTitleInput,
                onValueChange = {
                  customTitleInput = it
                  viewModel.updateSlotCustomization(
                    position = activeSlot.position,
                    customTitle = it,
                    customValue = customValueInput,
                    showInAmbient = activeSlot.showInAmbient,
                  )
                },
                label = { Text("表示ラベル上書き") },
                placeholder = { Text("自動") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = customValueInput,
                onValueChange = {
                  customValueInput = it
                  viewModel.updateSlotCustomization(
                    position = activeSlot.position,
                    customTitle = customTitleInput,
                    customValue = it,
                    showInAmbient = activeSlot.showInAmbient,
                  )
                },
                label = { Text("表示値/メモ上書き") },
                placeholder = { Text("自動") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
            }

            ToggleOptionRow(
              title = "AOD (常時表示) 中も表示",
              subtitle = "OFFにすると待機時は非表示になり電池消費を抑えます",
              checked = activeSlot.showInAmbient,
              onCheckedChange = { checked ->
                viewModel.updateSlotCustomization(
                  position = activeSlot.position,
                  customTitle = customTitleInput,
                  customValue = customValueInput,
                  showInAmbient = checked,
                )
              },
            )
          }
        }
      }
    }

    // 5. App Dock Shortcuts & Visibility Customization
    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Watchホーム・アプリドック構成",
            style = MaterialTheme.typography.titleMedium,
          )
          ToggleOptionRow(
            title = "画面下にアプリドックボタンを表示",
            subtitle = "文字盤と背景だけを純粋に見せたい時はOFFにできます",
            checked = uiState.activeConfig.showAppDockButton,
            onCheckedChange = viewModel::setShowAppDockButton,
          )
          if (uiState.activeConfig.showAppDockButton) {
            Text(
              text = "ドック内に表示するクイック起動ショートカット：",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.activeConfig.appDockShortcuts.forEach { shortcut ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = resolvePluginIcon(shortcut.iconKey),
                    contentDescription = shortcut.label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Column {
                    Text(shortcut.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                      shortcut.packageHint,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Switch(
                  checked = shortcut.enabled,
                  onCheckedChange = { viewModel.toggleAppDockShortcut(shortcut.id, it) },
                )
              }
            }
          }
        }
      }
    }
  }

  if (showCustomExtDialog) {
    CustomExtensionBuilderDialog(
      selectedSlotLabel = activeSlot.position.label,
      onDismiss = { showCustomExtDialog = false },
      onCreate = { title, tag, defaultVal, unit, iconKey ->
        viewModel.registerCustomWidgetExtension(
          title = title,
          shortTag = tag,
          defaultValue = defaultVal,
          unit = unit,
          iconKey = iconKey,
          assignToSelectedSlot = true,
        )
        showCustomExtDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatchRow(
  selectedHex: Long,
  onSelect: (Long) -> Unit,
  tagPrefix: String,
) {
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    SharedWatchPalette.AccentSwatches.forEach { (label, hex) ->
      val isSelected = selectedHex == hex
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(Color(hex))
          .border(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.outline,
            shape = CircleShape,
          )
          .clickable { onSelect(hex) }
          .testTag("${tagPrefix}_$label"),
        contentAlignment = Alignment.Center,
      ) {
        if (isSelected) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = label,
            tint = Color.Black,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun LabeledSlider(
  label: String,
  valueText: String,
  value: Float,
  onValueChange: (Float) -> Unit,
  valueRange: ClosedFloatingPointRange<Float>,
  testTag: String? = null,
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(label, style = MaterialTheme.typography.bodySmall)
      Text(
        text = valueText,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
    )
  }
}

@Composable
private fun ToggleOptionRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomExtensionBuilderDialog(
  selectedSlotLabel: String,
  onDismiss: () -> Unit,
  onCreate: (String, String, String, String, String) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var shortTag by remember { mutableStateOf("EXT") }
  var defaultValue by remember { mutableStateOf("99") }
  var unit by remember { mutableStateOf("%") }
  var iconKey by remember { mutableStateOf("spark") }

  val iconChoices = listOf(
    "spark" to "汎用",
    "notifications" to "通知",
    "fitness" to "運動",
    "music" to "音楽",
    "timer" to "タイマー",
    "weather" to "天気",
    "phone" to "スマホ",
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Rounded.Extension,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = { Text("カスタム拡張プラグイン作成") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "WatchExtensionRegistry へ独自のウィジェットプラグインを動的登録し、「$selectedSlotLabel」へ割り当てます。",
          style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("プラグイン名") },
          placeholder = { Text("例: カフェイン摂取 / 集中カウント") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("custom_ext_title_input"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = shortTag,
            onValueChange = { shortTag = it },
            label = { Text("短縮タグ") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = defaultValue,
            onValueChange = { defaultValue = it },
            label = { Text("初期値") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = unit,
            onValueChange = { unit = it },
            label = { Text("単位") },
            singleLine = true,
            modifier = Modifier.weight(0.7f),
          )
        }
        Text("アイコン選択", style = MaterialTheme.typography.bodySmall)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          iconChoices.forEach { (key, label) ->
            FilterChip(
              selected = iconKey == key,
              onClick = { iconKey = key },
              leadingIcon = {
                Icon(
                  imageVector = resolvePluginIcon(key),
                  contentDescription = label,
                  modifier = Modifier.size(15.dp),
                )
              },
              label = { Text(label) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onCreate(title, shortTag, defaultValue, unit, iconKey) },
        modifier = Modifier.testTag("confirm_create_custom_ext"),
      ) {
        Text("登録して適用")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("キャンセル")
      }
    },
  )
}
