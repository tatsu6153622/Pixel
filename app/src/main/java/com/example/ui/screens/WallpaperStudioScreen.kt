package com.example.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WatchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.chronocanvas.shared.model.ShaderPresetId
import com.aistudio.chronocanvas.shared.model.WallpaperSourceType
import com.example.R
import com.example.ui.components.InteractiveWatchStage
import com.example.ui.studio.StudioUiState
import com.example.ui.studio.StudioViewModel
import com.example.wallpaper.VideoLiveWallpaperService

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallpaperStudioScreen(
  uiState: StudioUiState,
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val videoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      viewModel.onVideoPickedFromPhotoPicker(uri)
    }
  }
  val offWristVideoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      viewModel.onOffWristVideoPicked(uri)
    }
  }

  val wp = uiState.activeConfig.wallpaper
  val auto = uiState.activeConfig.wallpaperAutomation

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Live Watch Preview showing actual Media3 ExoPlayer video, off-wrist, or time-of-day switch
    item {
      InteractiveWatchStage(
        uiState = uiState,
        onSelectModel = viewModel::setWatchModel,
        onToggleAmbient = viewModel::toggleAmbientPreview,
        onToggleAppDock = viewModel::toggleAppDockPreview,
        onOpenFullscreen = { viewModel.setFullscreenWatchMode(true) },
        onAppShortcutMessage = viewModel::showMessage,
        onToggleOffWrist = viewModel::toggleSimOffWrist,
        onTestNotification = viewModel::triggerTestNotificationPulse,
        onSelectPreviewHour = viewModel::setPreviewHourOverride,
        watchDiameter = 228.dp,
        showControls = true,
      )
    }

    // 1. Source Type Selector
    item {
      Text("壁紙ソースタイプ", style = MaterialTheme.typography.titleLarge)
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        WallpaperSourceType.entries.forEach { src ->
          FilterChip(
            selected = wp.sourceType == src,
            onClick = { viewModel.setWallpaperSourceType(src) },
            label = { Text(src.label) },
            modifier = Modifier
              .weight(1f)
              .testTag("wp_source_${src.name}"),
          )
        }
      }
    }

    // 2. Video Wallpaper Picker & Base-System Extension Card
    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
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
              imageVector = Icons.Rounded.Movie,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "動画壁紙 & 既存システム拡張",
              style = MaterialTheme.typography.titleMedium,
            )
          }

          Text(
            text = "写真しか選べない既存のベース壁紙システムに対し、Android Photo Picker（ストレージ権限不要）から選んだMP4/MOVをライブ壁紙拡張およびWatch用最適化フレーム列として適用できます。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = {
                videoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("pick_video_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("動画を選択")
            }

            OutlinedButton(
              onClick = {
                val component = ComponentName(context, VideoLiveWallpaperService::class.java)
                val liveIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                  putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                }
                val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                val launched = runCatching {
                  context.startActivity(liveIntent)
                  true
                }.recoverCatching {
                  context.startActivity(fallbackIntent)
                  true
                }.getOrDefault(false)
                if (!launched) {
                  viewModel.showMessage("この端末ではシステム壁紙ピッカーを開けませんでした")
                }
              },
              modifier = Modifier
                .weight(1f)
                .testTag("apply_system_live_wallpaper_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Wallpaper,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("OS壁紙拡張へ")
            }
          }

          if (uiState.isProcessingVideoExtension) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              Text(
                text = "動画をWatch解像度へ解析・最適化中...",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          } else if (wp.videoUriString.isNotBlank()) {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "選択中: ${wp.videoTitle}",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                uiState.selectedVideoDetails?.let { details ->
                  Text(
                    text = details.formattedSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                uiState.videoExtensionPackage?.let { pkg ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "既存Watchベース向け変換: ${pkg.targetResolutionPx}px · ${pkg.targetFps}fps · 約${pkg.estimatedPackageKb}KB · OLED黒率${pkg.oledDarkPixelRatioPct}%",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                      onClick = viewModel::regenerateFrameExtensionPreview,
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "フレーム再解析",
                        modifier = Modifier.size(14.dp),
                      )
                    }
                  }
                  LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    items(pkg.frameThumbnails) { bmp ->
                      Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "抽出Watchキーフレーム",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                          .size(54.dp)
                          .clip(RoundedCornerShape(10.dp)),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. Built-in Procedural Shaders & Generated Art Presets
    item {
      Text("組み込みシェーダー & プリセットアート", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShaderPresetId.entries.forEach { shader ->
          val selected = wp.sourceType == WallpaperSourceType.PROCEDURAL_SHADER && wp.shaderPresetId == shader
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .clickable { viewModel.setShaderPreset(shader) }
              .testTag("shader_preset_${shader.name}"),
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
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Column {
                Text(shader.title, style = MaterialTheme.typography.titleMedium)
                Text(
                  shader.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ArtPresetTile(
          title = "シアン・オーロラ",
          resId = R.drawable.img_preset_aurora_1795588541917,
          selected = wp.sourceType == WallpaperSourceType.PRESET_ART && wp.presetArtId == "preset_aurora",
          onClick = { viewModel.setPresetArt("preset_aurora") },
          modifier = Modifier.weight(1f),
        )
        ArtPresetTile(
          title = "カーボン・ホロジー",
          resId = R.drawable.img_preset_horology_1795588542472,
          selected = wp.sourceType == WallpaperSourceType.PRESET_ART && wp.presetArtId == "preset_horology",
          onClick = { viewModel.setPresetArt("preset_horology") },
          modifier = Modifier.weight(1f),
        )
      }
    }

    // 4. Off-Wrist Automatic Wallpaper Switching Card
    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Rounded.WatchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
              Column {
                Text("腕時計を外した時の壁紙切替", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "Pixel Watch 3の着脱センサー(Off-Body)連動",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Switch(
              checked = auto.switchOnOffWrist,
              onCheckedChange = { viewModel.updateOffWristAutomation(enabled = it) },
              modifier = Modifier.testTag("switch_offwrist_wallpaper"),
            )
          }

          if (auto.switchOnOffWrist) {
            Text(
              text = "腕から外した時に表示する背景タイプ：",
              style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              WallpaperSourceType.entries.forEach { src ->
                FilterChip(
                  selected = auto.offWristSourceType == src,
                  onClick = { viewModel.updateOffWristAutomation(sourceType = src) },
                  label = { Text(src.label) },
                )
              }
            }

            when (auto.offWristSourceType) {
              WallpaperSourceType.PROCEDURAL_SHADER -> {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  ShaderPresetId.entries.forEach { sh ->
                    FilterChip(
                      selected = auto.offWristShaderPreset == sh,
                      onClick = { viewModel.updateOffWristAutomation(shaderPreset = sh) },
                      label = { Text(sh.title) },
                    )
                  }
                }
              }

              WallpaperSourceType.PRESET_ART -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  FilterChip(
                    selected = auto.offWristPresetArtId == "preset_aurora",
                    onClick = { viewModel.updateOffWristAutomation(presetArtId = "preset_aurora") },
                    label = { Text("シアン・オーロラ") },
                  )
                  FilterChip(
                    selected = auto.offWristPresetArtId == "preset_horology",
                    onClick = { viewModel.updateOffWristAutomation(presetArtId = "preset_horology") },
                    label = { Text("カーボン・ホロジー") },
                  )
                }
              }

              WallpaperSourceType.LOCAL_VIDEO_URI -> {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "外した時用動画: ${auto.offWristVideoTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                  )
                  OutlinedButton(
                    onClick = {
                      offWristVideoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                      )
                    },
                    modifier = Modifier.testTag("pick_offwrist_video_button"),
                  ) {
                    Text("動画を選ぶ")
                  }
                }
              }
            }

            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("外した時の減光レベル", style = MaterialTheme.typography.bodySmall)
                Text(
                  text = "${(auto.offWristDimOpacity * 100).toInt()}%",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Slider(
                value = auto.offWristDimOpacity,
                onValueChange = { viewModel.updateOffWristAutomation(dimOpacity = it) },
                valueRange = 0f..0.90f,
              )
            }

            OutlinedButton(
              onClick = { viewModel.toggleSimOffWrist(!uiState.telemetry.isOffWrist) },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("preview_offwrist_toggle"),
            ) {
              Text(
                if (uiState.telemetry.isOffWrist) "プレビューを「装着中」に戻す"
                else "プレビューで「外した状態」を確認する",
              )
            }
          }
        }
      }
    }

    // 5. Time-of-Day Automatic Wallpaper Switching Card
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Column {
                Text("時間帯による壁紙自動切替", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "朝・昼・夜・深夜で背景と明るさを自動切り替え",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Switch(
              checked = auto.timeScheduleEnabled,
              onCheckedChange = viewModel::toggleTimeSchedule,
              modifier = Modifier.testTag("switch_time_schedule"),
            )
          }

          if (auto.timeScheduleEnabled) {
            Text(
              text = "テストプレビュー時間帯：",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              listOf(
                null to "実時刻",
                9 to "朝 09時",
                15 to "昼 15時",
                20 to "夜 20時",
                2 to "深夜 02時",
              ).forEach { (h, label) ->
                FilterChip(
                  selected = uiState.previewHourOverride == h,
                  onClick = { viewModel.setPreviewHourOverride(h) },
                  label = { Text(label) },
                )
              }
            }

            auto.timeRules.forEach { rule ->
              Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = rule.period.label,
                      style = MaterialTheme.typography.titleSmall,
                      color = if (rule.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                      checked = rule.enabled,
                      onCheckedChange = { enabled ->
                        viewModel.updateTimeRule(rule.period, enabled = enabled)
                      },
                    )
                  }
                  if (rule.enabled) {
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      ShaderPresetId.entries.forEach { sh ->
                        FilterChip(
                          selected = rule.sourceType == WallpaperSourceType.PROCEDURAL_SHADER && rule.shaderPresetId == sh,
                          onClick = {
                            viewModel.updateTimeRule(
                              period = rule.period,
                              sourceType = WallpaperSourceType.PROCEDURAL_SHADER,
                              shaderPresetId = sh,
                            )
                          },
                          label = { Text(sh.title) },
                        )
                      }
                      FilterChip(
                        selected = rule.sourceType == WallpaperSourceType.PRESET_ART && rule.presetArtId == "preset_aurora",
                        onClick = {
                          viewModel.updateTimeRule(
                            period = rule.period,
                            sourceType = WallpaperSourceType.PRESET_ART,
                            presetArtId = "preset_aurora",
                          )
                        },
                        label = { Text("オーロラ画像") },
                      )
                      FilterChip(
                        selected = rule.sourceType == WallpaperSourceType.PRESET_ART && rule.presetArtId == "preset_horology",
                        onClick = {
                          viewModel.updateTimeRule(
                            period = rule.period,
                            sourceType = WallpaperSourceType.PRESET_ART,
                            presetArtId = "preset_horology",
                          )
                        },
                        label = { Text("ホロジー画像") },
                      )
                      if (wp.videoUriString.isNotBlank()) {
                        FilterChip(
                          selected = rule.sourceType == WallpaperSourceType.LOCAL_VIDEO_URI,
                          onClick = {
                            viewModel.updateTimeRule(
                              period = rule.period,
                              sourceType = WallpaperSourceType.LOCAL_VIDEO_URI,
                            )
                          },
                          label = { Text("選択中の動画") },
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // 6. Watch Export & OLED Power Tuning
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
          Text("動画・背景のWatch最適化パラメータ", style = MaterialTheme.typography.titleMedium)

          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("OLED黒マスク減光（視認性＆節電）", style = MaterialTheme.typography.bodyMedium)
              Text(
                text = "${(wp.dimOpacity * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Slider(
              value = wp.dimOpacity,
              onValueChange = { viewModel.updateWallpaperSettings(dimOpacity = it) },
              valueRange = 0f..0.85f,
            )
          }

          Text("転送ターゲットFPS上限", style = MaterialTheme.typography.bodySmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 24, 30).forEach { fps ->
              FilterChip(
                selected = wp.targetFps == fps,
                onClick = { viewModel.updateWallpaperSettings(targetFps = fps) },
                label = { Text("${fps}fps") },
              )
            }
          }

          Text("Watchリサイズ解像度", style = MaterialTheme.typography.bodySmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(360 to "360px (最軽量)", 408 to "408px (41mm)", 456 to "456px (45mm)").forEach { (px, label) ->
              FilterChip(
                selected = wp.downscalePx == px,
                onClick = { viewModel.updateWallpaperSettings(downscalePx = px) },
                label = { Text(label) },
              )
            }
          }

          Text("再生速度", style = MaterialTheme.typography.bodySmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.5f to "0.5x スロー", 1.0f to "1.0x 標準", 1.5f to "1.5x").forEach { (spd, label) ->
              FilterChip(
                selected = wp.playbackSpeed == spd,
                onClick = { viewModel.updateWallpaperSettings(playbackSpeed = spd) },
                label = { Text(label) },
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("シームレスループ再生", style = MaterialTheme.typography.bodyMedium)
              Text(
                "動画を終端で自動的に繰り返します",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = wp.loopEnabled,
              onCheckedChange = { viewModel.updateWallpaperSettings(loopEnabled = it) },
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("AOD (常時表示) 時は動画を自動停止", style = MaterialTheme.typography.bodyMedium)
              Text(
                "Pixel Watch 3の消費電力を劇的に削減します",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = wp.pauseInAmbient,
              onCheckedChange = { viewModel.updateWallpaperSettings(pauseInAmbient = it) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ArtPresetTile(
  title: String,
  resId: Int,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(
      width = if (selected) 2.dp else 1.dp,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
    ),
  ) {
    Column {
      Image(
        painter = painterResource(id = resId),
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(92.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(10.dp),
      )
    }
  }
}
