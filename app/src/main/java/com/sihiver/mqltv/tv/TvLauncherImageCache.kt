package com.sihiver.mqltv.tv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.sihiver.mqltv.R

/** Bitmap logo saluran launcher (ic_channel penuh, bukan adaptive-icon). */
object TvLauncherImageCache {

    fun createAppChannelLogoBitmap(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = maxOf((80 * density).toInt(), 512)

        val source = BitmapFactory.decodeResource(context.resources, R.drawable.ic_channel_logo)
        if (source != null) {
            val scaled = Bitmap.createScaledBitmap(source, size, size, true)
            if (scaled != source) source.recycle()
            return scaled
        }

        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ContextCompat.getColor(context, R.color.ic_channel_background))
        val foreground = ContextCompat.getDrawable(context, R.mipmap.ic_channel_foreground)
        if (foreground != null) {
            foreground.setBounds(0, 0, size, size)
            foreground.draw(canvas)
        }
        return bitmap
    }
}
