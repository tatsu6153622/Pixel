package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FullscreenWatchSimulatorDialog
import com.example.ui.screens.BatteryAndSyncScreen
import com.example.ui.screens.ClockAndWidgetScreen
import com.example.ui.screens.GuideDocsScreen
import com.example.ui.screens.StudioHomeScreen
import com.example.ui.screens.WallpaperStudioScreen
import com.example.ui.studio.StudioTab
import com.example.ui.studio.StudioViewModel
import com.example.ui.theme.ChronoCanvasTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ChronoCanvasTheme {
        ChronoCanvasApp()
      }
    }
  }
}

@Composable
fun ChronoCanvasApp(
  viewModel: StudioViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var currentTab by rememberSaveable { mutableStateOf(StudioTab.STUDIO) }
  val snackbarHostState = remember { SnackbarHostState() }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshNotificationPermissionStatus()
        viewModel.syncLiveTelemetryNow()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Back button from secondary tabs returns to Studio Home
  BackHandler(enabled = currentTab != StudioTab.STUDIO) {
    currentTab = StudioTab.STUDIO
  }

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            StudioTab.entries.forEach { tab ->
              NavigationBarItem(
                selected = currentTab == tab,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab),
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label, maxLines = 1) },
                modifier = Modifier.testTag("nav_${tab.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useNavigationRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .fillMaxHeight()
              .windowInsetsPadding(WindowInsets.safeDrawing),
          ) {
            StudioTab.entries.forEach { tab ->
              NavigationRailItem(
                selected = currentTab == tab,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab),
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("rail_${tab.route}"),
              )
            }
          }
        }

        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.TopCenter,
        ) {
          val contentModifier = Modifier
            .widthIn(max = 840.dp)
            .fillMaxSize()

          when (currentTab) {
            StudioTab.STUDIO -> StudioHomeScreen(
              uiState = uiState,
              onSelectModel = viewModel::setWatchModel,
              onToggleAmbient = viewModel::toggleAmbientPreview,
              onToggleAppDock = viewModel::toggleAppDockPreview,
              onOpenFullscreen = { viewModel.setFullscreenWatchMode(true) },
              onSlotTappedToEdit = viewModel::selectSlotForEdit,
              onLoadPreset = viewModel::loadPreset,
              onSavePreset = viewModel::saveCurrentAsPreset,
              onDeletePreset = viewModel::deleteCustomPreset,
              onSyncNow = viewModel::syncNowToPixelWatch,
              onNavigateTab = { currentTab = it },
              onShowMessage = viewModel::showMessage,
              onToggleOffWrist = viewModel::toggleSimOffWrist,
              onTestNotification = viewModel::triggerTestNotificationPulse,
              onSelectPreviewHour = viewModel::setPreviewHourOverride,
              modifier = contentModifier,
            )

            StudioTab.CLOCK_WIDGETS -> ClockAndWidgetScreen(
              uiState = uiState,
              viewModel = viewModel,
              modifier = contentModifier,
            )

            StudioTab.WALLPAPER -> WallpaperStudioScreen(
              uiState = uiState,
              viewModel = viewModel,
              modifier = contentModifier,
            )

            StudioTab.BATTERY_SYNC -> BatteryAndSyncScreen(
              uiState = uiState,
              viewModel = viewModel,
              modifier = contentModifier,
            )

            StudioTab.GUIDE -> GuideDocsScreen(
              onNavigateTab = { currentTab = it },
              modifier = contentModifier,
            )
          }
        }
      }
    }
  }

  if (uiState.isFullscreenWatchMode) {
    FullscreenWatchSimulatorDialog(
      uiState = uiState,
      onDismiss = { viewModel.setFullscreenWatchMode(false) },
      onToggleAmbient = viewModel::toggleAmbientPreview,
      onToggleAppDock = viewModel::toggleAppDockPreview,
      onAppShortcutMessage = viewModel::showMessage,
      onToggleOffWrist = viewModel::toggleSimOffWrist,
      onTestNotification = viewModel::triggerTestNotificationPulse,
    )
  }
}

private fun tabIcon(tab: StudioTab): ImageVector = when (tab) {
  StudioTab.STUDIO -> Icons.Rounded.Watch
  StudioTab.CLOCK_WIDGETS -> Icons.Rounded.Tune
  StudioTab.WALLPAPER -> Icons.Rounded.Movie
  StudioTab.BATTERY_SYNC -> Icons.Rounded.BatteryChargingFull
  StudioTab.GUIDE -> Icons.Rounded.MenuBook
}
