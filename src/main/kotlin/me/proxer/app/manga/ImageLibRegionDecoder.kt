package me.proxer.app.manga

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import androidx.annotation.Keep
import androidx.core.net.toFile
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
import com.hippo.image.BitmapRegionDecoder

/**
 * @author Ruben Gees
 */
class ImageLibRegionDecoder @JvmOverloads @Keep constructor(bitmapConfig: Bitmap.Config? = null) : ImageRegionDecoder {

    private val bitmapConfig: Int
    private var decoder: BitmapRegionDecoder? = null

    init {
        val globalBitmapConfig = SubsamplingScaleImageView.getPreferredBitmapConfig()

        val bitmapConfigToUse = when {
            bitmapConfig != null -> bitmapConfig
            globalBitmapConfig != null -> globalBitmapConfig
            else -> com.hippo.image.BitmapDecoder.CONFIG_RGB_565
        }

        this.bitmapConfig = when (bitmapConfigToUse) {
            Bitmap.Config.RGB_565 -> com.hippo.image.BitmapDecoder.CONFIG_RGB_565
            Bitmap.Config.ARGB_8888 -> com.hippo.image.BitmapDecoder.CONFIG_RGBA_8888
            else -> com.hippo.image.BitmapDecoder.CONFIG_RGB_565
        }
    }

    override fun isReady() = decoder != null

    override fun init(context: Context?, uri: Uri): Point {
        decoder = BitmapRegionDecoder.newInstance(uri.toFile().inputStream())

        return Point(decoder?.width ?: -1, decoder?.height ?: -1)
    }

    override fun decodeRegion(sRect: Rect, sampleSize: Int): Bitmap {
        return requireNotNull(decoder?.decodeRegion(sRect, bitmapConfig, sampleSize))
    }

    override fun recycle() {
        decoder?.recycle()
        decoder = null
    }
}
