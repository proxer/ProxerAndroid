package me.proxer.app.manga

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
import rapid.decoder.BitmapDecoder

/**
 * @author Ruben Gees
 */
class RapidImageRegionDecoder : ImageRegionDecoder {

    private var decoder: BitmapDecoder? = null

    override fun init(context: Context, uri: Uri): Point {
        decoder = BitmapDecoder.from(context, uri)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        config(Bitmap.Config.HARDWARE)
                    } else {
                        config(Bitmap.Config.ARGB_8888)
                    }
                }

        return Point(decoder?.sourceWidth() ?: -1, decoder?.sourceHeight() ?: -1)
    }

    @Synchronized override fun decodeRegion(sRect: Rect, sampleSize: Int): Bitmap? {
        return try {
            decoder?.reset()
                    ?.region(sRect)
                    ?.scale(sRect.width() / sampleSize, sRect.height() / sampleSize)
                    ?.decode()
        } catch (error: Exception) {
            null
        }
    }

    override fun isReady() = decoder != null

    override fun recycle() {
        BitmapDecoder.destroyMemoryCache()
        BitmapDecoder.destroyDiskCache()

        decoder?.reset()
        decoder = null
    }
}
