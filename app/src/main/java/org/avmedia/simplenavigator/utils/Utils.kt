package org.avmedia.simplenavigator.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.ToneGenerator
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object Utils {
    fun getMarkerIconFromDrawable(drawable: Drawable?): BitmapDescriptor? {
        if (drawable == null) {
            return null
        }
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        canvas.setBitmap(bitmap)

        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    enum class TONE {
        PIP,
        ALERT,
        INTERCEPT
    }

    fun beep(tone: TONE, duration: Int = 150) {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        val tgTone = when (tone) {
            TONE.INTERCEPT -> {
                ToneGenerator.TONE_CDMA_ABBR_INTERCEPT
            }
            TONE.ALERT -> {
                ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            }
            else -> {
                ToneGenerator.TONE_CDMA_PIP
            }
        }
        toneGen.startTone(tgTone, duration)
    }
}