package com.aistudio.chronocanvas.wear

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/**
 * Exposes a 1-tap "ChronoCanvas" complication so users can launch their custom watch home
 * directly from any Pixel Watch 3 system watch face slot.
 */
class ChronoShortcutComplicationService : SuspendingComplicationDataSourceService() {

  override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
    val tapIntent = PendingIntent.getActivity(
      this,
      303,
      Intent(this, WatchHomeActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return buildComplicationData(request.complicationType, tapIntent)
  }

  override fun getPreviewData(type: ComplicationType): ComplicationData? {
    return buildComplicationData(type, tapAction = null)
  }

  private fun buildComplicationData(
    type: ComplicationType,
    tapAction: PendingIntent?,
  ): ComplicationData? {
    val icon = MonochromaticImage.Builder(
      Icon.createWithResource(this, R.drawable.ic_watch_ongoing),
    ).build()
    val contentDesc = PlainComplicationText.Builder("ChronoCanvas ホームを開く").build()

    return when (type) {
      ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
        text = PlainComplicationText.Builder("HOME").build(),
        contentDescription = contentDesc,
      )
        .setMonochromaticImage(icon)
        .setTapAction(tapAction)
        .build()

      ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
        monochromaticImage = icon,
        contentDescription = contentDesc,
      )
        .setTapAction(tapAction)
        .build()

      else -> null
    }
  }
}
