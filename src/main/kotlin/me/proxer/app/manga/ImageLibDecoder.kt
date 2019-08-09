package me.proxer.app.manga

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.Keep
import androidx.core.net.toFile
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder
import com.hippo.image.BitmapDecoder

/**
 * @author Ruben Gees
 */
class ImageLibDecoder @JvmOverloads @Keep constructor(bitmapConfig: Bitmap.Config? = null) : ImageDecoder {

    private val bitmapConfig: Int

    init {
        val globalBitmapConfig = SubsamplingScaleImageView.getPreferredBitmapConfig()

        val bitmapConfigToUse = when {
            bitmapConfig != null -> bitmapConfig
            globalBitmapConfig != null -> globalBitmapConfig
            else -> BitmapDecoder.CONFIG_RGB_565
        }

        this.bitmapConfig = when (bitmapConfigToUse) {
            Bitmap.Config.RGB_565 -> BitmapDecoder.CONFIG_RGB_565
            Bitmap.Config.ARGB_8888 -> BitmapDecoder.CONFIG_RGBA_8888
            else -> BitmapDecoder.CONFIG_RGB_565
        }
    }

    override fun decode(context: Context?, uri: Uri): Bitmap {
        return uri.toFile().inputStream()
            .use { BitmapDecoder.decode(it) }
            .let { requireNotNull(it) }
    }
}
