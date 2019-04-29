package me.proxer.app.manga.decoder

import android.graphics.Bitmap
import com.davemorrissey.labs.subscaleview.decoder.DecoderFactory
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder

/**
 * @author Ruben Gees
 */
class FallbackRegionDecoderFactory @JvmOverloads constructor(
    private val bitmapConfig: Bitmap.Config? = null
) : FallbackManager(), DecoderFactory<ImageRegionDecoder> {

    override fun make() = when (fallbackMap[nextKey] ?: FallbackStage.NORMAL) {
        FallbackStage.NORMAL -> SkiaPooledImageRegionDecoder(bitmapConfig)
        FallbackStage.RAPID -> RapidImageRegionDecoder(bitmapConfig)
        FallbackStage.NATIVE -> RapidImageRegionNativeDecoder(bitmapConfig)
    }
}
