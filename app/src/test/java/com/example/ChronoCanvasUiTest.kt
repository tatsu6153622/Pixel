package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChronoCanvasUiTest {

  @get:Rule
  val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun navigationAcrossTabsAndGuideSearchWorks() {
    composeRule.onNodeWithTag("watch_live_canvas").assertIsDisplayed()

    composeRule.onNodeWithTag("nav_guide").performClick()
    composeRule.onNodeWithTag("guide_search_input").performTextInput("動画")
    composeRule.onNodeWithText("既存の「写真のみ壁紙」を動画対応にする拡張機能の仕組み").assertIsDisplayed()

    composeRule.onNodeWithTag("nav_battery_sync").performClick()
    composeRule.onNodeWithTag("sync_now_button").assertIsDisplayed()

    composeRule.onNodeWithTag("nav_wallpaper").performClick()
    composeRule.onNodeWithTag("pick_video_button").assertIsDisplayed()
    composeRule.onNodeWithTag("apply_system_live_wallpaper_button").assertIsDisplayed()
  }
}
