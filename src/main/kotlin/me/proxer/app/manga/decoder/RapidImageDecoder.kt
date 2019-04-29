package me.proxer.app.manga.decoder

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.Keep
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder
import rapid.decoder.BitmapDecoder

/**
 * @author Ruben Gees
 */

class RapidImageDecoder @JvmOverloads @Keep constructor(bitmapConfig: Bitmap.Config? = null) : ImageDecoder {

    private val bitmapConfig: Bitmap.Config

    init {
        val globalBitmapConfig = SubsamplingScaleImageView.getPreferredBitmapConfig()

        this.bitmapConfig = when {
            bitmapConfig != null -> bitmapConfig
            globalBitmapConfig != null -> globalBitmapConfig
            else -> Bitmap.Config.RGB_565
        }
    }

    override fun decode(context: Context, uri: Uri) = BitmapDecoder.from(context, uri)
        .config(bitmapConfig)
        .mutable(false)
        .decode()
        ?: throw IllegalStateException("decoded bitmap is null")
}
