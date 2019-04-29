package me.proxer.app.manga.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import androidx.annotation.Keep
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
import rapid.decoder.BitmapDecoder

/**
 * @author Ruben Gees
 */
class RapidImageRegionDecoder @JvmOverloads @Keep constructor(
    bitmapConfig: Bitmap.Config? = null
) : ImageRegionDecoder {

    private val bitmapConfig: Bitmap.Config
    private var decoder: BitmapDecoder? = null

    init {
        val globalBitmapConfig = SubsamplingScaleImageView.getPreferredBitmapConfig()

        this.bitmapConfig = when {
            bitmapConfig != null -> bitmapConfig
            globalBitmapConfig != null -> globalBitmapConfig
            else -> Bitmap.Config.RGB_565
        }
    }

    override fun init(context: Context, uri: Uri): Point {
        decoder = BitmapDecoder.from(context, uri)

        return Point(decoder?.sourceWidth() ?: -1, decoder?.sourceHeight() ?: -1)
    }

    @Synchronized
    override fun decodeRegion(sRect: Rect, sampleSize: Int) = decoder?.reset()
        ?.config(bitmapConfig)
        ?.region(sRect)
        ?.scale(sRect.width() / sampleSize, sRect.height() / sampleSize)
        ?.mutable(false)
        ?.decode()
        ?: throw IllegalStateException("decoded bitmap is null")

    override fun isReady() = decoder != null

    override fun recycle() {
        BitmapDecoder.destroyMemoryCache()
        BitmapDecoder.destroyDiskCache()

        decoder?.cancel()
        decoder?.reset()
        decoder = null
    }
}
