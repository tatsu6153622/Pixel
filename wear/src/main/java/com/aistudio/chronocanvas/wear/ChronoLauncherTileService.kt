package com.aistudio.chronocanvas.wear

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.tile

/**
 * Wear OS Tile letting Pixel Watch 3 users swipe once from any system watch face
 * and launch straight into their ChronoCanvas video wallpaper & widget home.
 */
class ChronoLauncherTileService : Material3TileService() {

  override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
    WatchLocalStore.initialize(applicationContext)
    val config = WatchLocalStore.configState.value
    val subtitleText = if (config.wallpaper.videoTitle != "未選択") {
      "壁紙: ${config.wallpaper.videoTitle}"
    } else {
      "壁紙: ${config.wallpaper.shaderPresetId.title}"
    }

    val openHomeClickable = clickable(
      action = launchAction(
        ComponentName(
          packageName,
          WatchHomeActivity::class.java.name,
        ),
      ),
    )

    return tile(
      timeline = Timeline.fromLayoutElement(
        primaryLayout(
          titleSlot = {
            text(
              text = "ChronoCanvas".layoutString,
              typography = Typography.TITLE_MEDIUM,
            )
          },
          mainSlot = {
            text(
              text = subtitleText.layoutString,
              typography = Typography.BODY_MEDIUM,
              maxLines = 2,
            )
          },
          bottomSlot = {
            textEdgeButton(
              labelContent = { text("ホームを開く".layoutString) },
              onClick = openHomeClickable,
            )
          },
        ),
      ),
    )
  }
}
