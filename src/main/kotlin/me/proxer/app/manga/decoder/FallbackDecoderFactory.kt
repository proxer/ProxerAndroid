package me.proxer.app.manga.decoder

import android.graphics.Bitmap
import com.davemorrissey.labs.subscaleview.decoder.DecoderFactory
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder

/**
 * @author Ruben Gees
 */
class FallbackDecoderFactory @JvmOverloads constructor(
    private val bitmapConfig: Bitmap.Config? = null
) : FallbackManager(), DecoderFactory<ImageDecoder> {

    override fun make() = when (fallbackMap[nextKey] ?: FallbackStage.NORMAL) {
        FallbackStage.NORMAL -> SkiaImageDecoder(bitmapConfig)
        FallbackStage.RAPID -> RapidImageDecoder(bitmapConfig)
        FallbackStage.NATIVE -> RapidImageNativeDecoder(bitmapConfig)
    }
}
