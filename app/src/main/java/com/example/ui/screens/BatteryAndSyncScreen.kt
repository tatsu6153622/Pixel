package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.chronocanvas.shared.codec.WatchConfigCodec
import com.aistudio.chronocanvas.shared.model.BatteryProfile
import com.example.BuildConfig
import com.example.ui.studio.StudioUiState
import com.example.ui.studio.StudioViewModel
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatteryAndSyncScreen(
  uiState: StudioUiState,
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier,
) {
  var showPayloadDialog by remember { mutableStateOf(false) }
  var customReleaseUrlInput by remember { mutableStateOf("") }
  val report = uiState.powerReport
  val bat = uiState.activeConfig.battery

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Power Efficiency Gauge & Breakdown
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.BatterySaver,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text("バッテリー最適化アナライザ", style = MaterialTheme.typography.titleMedium)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${uiState.activeConfig.watchModel.displayName} (${uiState.activeConfig.watchModel.batteryCapacityMah}mAh)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "推定駆動 ${"%.1f".format(report.estimatedHours)} 時間",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "平均消費電流 ${"%.1f".format(report.averageDrainMa)} mA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            // Efficiency Arc Gauge
            Box(
              modifier = Modifier.size(104.dp),
              contentAlignment = Alignment.Center,
            ) {
              val primary = MaterialTheme.colorScheme.primary
              val track = MaterialTheme.colorScheme.outline
              Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                drawArc(
                  color = track,
                  startAngle = 135f,
                  sweepAngle = 270f,
                  useCenter = false,
                  style = Stroke(width = 12f, cap = StrokeCap.Round),
                )
                drawArc(
                  color = primary,
                  startAngle = 135f,
                  sweepAngle = 270f * (report.efficiencyScore / 100f),
                  useCenter = false,
                  style = Stroke(width = 12f, cap = StrokeCap.Round),
                )
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "${report.efficiencyScore}",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "SCORE",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          PowerBreakdownBar("動画/背景デコード", report.videoDecodeMa, report.averageDrainMa, MaterialTheme.colorScheme.tertiary)
          PowerBreakdownBar("OLEDピクセル発光", report.oledDisplayMa, report.averageDrainMa, MaterialTheme.colorScheme.secondary)
          PowerBreakdownBar("LTPO 常時表示 (AOD)", report.ambientBaselineMa, report.averageDrainMa, MaterialTheme.colorScheme.primary)
          PowerBreakdownBar("センサー・ウィジェット更新", report.sensorPollingMa, report.averageDrainMa, Color(0xFF9E8CFF))

          // Optimization Tips
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.TipsAndUpdates,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(16.dp),
                )
                Text("最適化診断アドバイス", style = MaterialTheme.typography.labelLarge)
              }
              report.optimizationTips.forEach { tip ->
                Text(
                  text = "• $tip",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }

    // 2. Optimization Profiles
    item {
      Text("ワンタップ省電力プロファイル", style = MaterialTheme.typography.titleLarge)
      Spacer(modifier = Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
          BatteryProfile.ULTRA_ENDURANCE,
          BatteryProfile.BALANCED,
          BatteryProfile.MAX_VISUAL,
        ).forEach { profile ->
          val selected = bat.profile == profile
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .clickable { viewModel.applyBatteryProfile(profile) }
              .testTag("battery_profile_${profile.name}"),
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
            Column(modifier = Modifier.padding(14.dp)) {
              Text(profile.title, style = MaterialTheme.typography.titleMedium)
              Text(
                profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    // 3. Granular Optimization Controls
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
          Text("省電力詳細コントロール", style = MaterialTheme.typography.titleMedium)
          BatterySwitchRow(
            title = "LTPO 1Hz アンビエント制御",
            subtitle = "Pixel Watch 3のAOD時に最小リフレッシュレートへ固定",
            checked = bat.ltpoAmbient1Hz,
            onCheckedChange = { viewModel.updateBatteryFlags(ltpoAmbient1Hz = it) },
          )
          BatterySwitchRow(
            title = "AOD中の動画壁紙を一時停止",
            subtitle = "スリープ中の動画デコードを停止し電池消費を抑制",
            checked = bat.pauseVideoInAod,
            onCheckedChange = { viewModel.updateBatteryFlags(pauseVideoInAod = it) },
          )
          BatterySwitchRow(
            title = "充電中のみ大容量動画を自動同期",
            subtitle = "Bluetooth帯域と時計側バッテリー消耗を保護",
            checked = bat.syncOnlyOnCharger,
            onCheckedChange = { viewModel.updateBatteryFlags(syncOnlyOnCharger = it) },
          )

          Text("ウィジェット更新間隔", style = MaterialTheme.typography.bodySmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15 to "15秒", 30 to "30秒 (推奨)", 60 to "60秒", 120 to "120秒").forEach { (sec, label) ->
              FilterChip(
                selected = bat.complicationRefreshSec == sec,
                onClick = { viewModel.updateBatteryFlags(complicationRefreshSec = sec) },
                label = { Text(label) },
              )
            }
          }
        }
      }
    }

    // 4. Pixel Watch 3 Wear OS Data Layer Sync & Auto-Update Center
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Watch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text("Pixel Watch 3 同期・OTA更新", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
              onClick = viewModel::refreshWearNodes,
              enabled = !uiState.isScanningNodes,
              modifier = Modifier.testTag("scan_nodes_button"),
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("再検索")
            }
          }

          BatterySwitchRow(
            title = "変更時にWatchへ自動OTA反映",
            subtitle = "時計・配置・通知発光・壁紙の変更をWatchアプリ再インストール不要で自動送信します",
            checked = uiState.autoSyncEnabled,
            onCheckedChange = viewModel::setAutoSyncEnabled,
          )

          // Phone vs Watch Version Handshake Card
          val watchVer = uiState.watchAppStatus
          val phoneVerCode = BuildConfig.VERSION_CODE
          val phoneVerName = BuildConfig.VERSION_NAME
          val isWatchOutdated = watchVer != null &&
            (watchVer.versionCode < phoneVerCode || watchVer.schemaVersion < WatchConfigCodec.SCHEMA_VERSION)

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isWatchOutdated) {
              MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            } else {
              MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
              1.dp,
              if (isWatchOutdated) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = if (isWatchOutdated) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "Watchバージョン連携状態",
                    style = MaterialTheme.typography.labelLarge,
                  )
                }
                Text(
                  text = when {
                    watchVer == null -> "未確認"
                    isWatchOutdated -> "要Watch更新"
                    else -> "OTA最新互換"
                  },
                  style = MaterialTheme.typography.labelMedium,
                  color = when {
                    watchVer == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    isWatchOutdated -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                  },
                )
              }

              Text(
                text = "スマホ側: v$phoneVerName (Code $phoneVerCode / Schema v${WatchConfigCodec.SCHEMA_VERSION})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = if (watchVer != null) {
                  "Watch実機 (${watchVer.deviceModel}): v${watchVer.versionName} (Code ${watchVer.versionCode} / Schema v${watchVer.schemaVersion})"
                } else {
                  "Watch実機: Watch側でChronoCanvasを一度開くとバージョンと対応状況が自動通知されます"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (watchVer != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
              )
              uiState.lastOtaSyncMs?.let { syncedAt ->
                val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                Text(
                  text = "最終OTA反映時刻: ${fmt.format(Date(syncedAt))}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }

          if (uiState.connectedNodes.isEmpty()) {
            Text(
              text = if (uiState.wearApiAvailable) {
                "Wear OS Data Layer APIは有効ですが、現在ペアリングされたPixel Watch 3ノードが検出されていません。"
              } else {
                "Wear OSノード未検出（エミュレータまたは単体端末環境）。実機ペアリング後にDataClient/ChannelClientで即時転送できます。"
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            uiState.connectedNodes.forEach { node ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text(node.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                      text = "Node: ${node.id} · ${if (node.isNearby) "Bluetooth近接" else "Cloud中継"}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = if (node.hasCompanionApp) "Watchアプリ導入済" else "未導入",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (node.hasCompanionApp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = viewModel::syncNowToPixelWatch,
              enabled = !uiState.isSyncing,
              modifier = Modifier
                .weight(1f)
                .testTag("sync_now_button"),
            ) {
              if (uiState.isSyncing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              } else {
                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text("全データ今すぐ同期")
            }

            OutlinedButton(
              onClick = { showPayloadDialog = true },
              modifier = Modifier.testTag("inspect_payload_button"),
            ) {
              Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("JSON")
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = { viewModel.launchRemoteWatchUpdate() },
              enabled = !uiState.isLaunchingRemoteUpdate,
              modifier = Modifier
                .weight(1f)
                .testTag("launch_watch_play_update_button"),
            ) {
              Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Watchでストア更新を開く")
            }

            OutlinedButton(
              onClick = viewModel::forceRefreshWatchUi,
              modifier = Modifier.testTag("force_refresh_watch_button"),
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Watch再読込")
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = customReleaseUrlInput,
              onValueChange = { customReleaseUrlInput = it },
              label = { Text("テスト配布リンク (Play内部テスト等)") },
              placeholder = { Text("https://play.google.com/...") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedButton(
              onClick = { viewModel.launchRemoteWatchUpdate(customReleaseUrlInput) },
              enabled = customReleaseUrlInput.isNotBlank() && !uiState.isLaunchingRemoteUpdate,
              modifier = Modifier.testTag("launch_custom_watch_url_button"),
            ) {
              Icon(Icons.Rounded.OpenInNew, contentDescription = "Watchで開く", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Watchへ")
            }
          }
        }
      }
    }

    // 5. Sync History Logs
    if (uiState.syncLogs.isNotEmpty()) {
      item {
        Text("同期ログ履歴", style = MaterialTheme.typography.titleMedium)
      }
      items(uiState.syncLogs, key = { it.id }) { log ->
        val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surfaceContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "${log.statusLabel} · ${log.targetNodeName}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "${fmt.format(Date(log.timestampMs))} (${log.payloadBytes}B)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = log.detailMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }

  if (showPayloadDialog) {
    val jsonStr = remember(uiState.activeConfig) {
      WatchConfigCodec.toJson(uiState.activeConfig, pretty = true)
    }
    AlertDialog(
      onDismissRequest = { showPayloadDialog = false },
      title = { Text("Wear OS Data Layer ペイロード") },
      text = {
        LazyColumn(modifier = Modifier.height(340.dp)) {
          item {
            Text(
              text = jsonStr,
              fontFamily = JetBrainsMonoFamily,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showPayloadDialog = false }) {
          Text("閉じる")
        }
      },
    )
  }
}

@Composable
private fun PowerBreakdownBar(
  label: String,
  valueMa: Float,
  totalMa: Float,
  color: Color,
) {
  val fraction = (valueMa / totalMa.coerceAtLeast(1f)).coerceIn(0f, 1f)
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(label, style = MaterialTheme.typography.bodySmall)
      Text(
        text = "${"%.1f".format(valueMa)} mA",
        style = MaterialTheme.typography.labelMedium,
        color = color,
      )
    }
    LinearProgressIndicator(
      progress = { fraction },
      color = color,
      trackColor = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(50)),
    )
  }
}

@Composable
private fun BatterySwitchRow(
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
      Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
