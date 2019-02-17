package me.proxer.app.manga.decoder

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder
import rapid.decoder.BitmapDecoder

/**
 * @author Ruben Gees
 */
class RapidImageNativeDecoder : ImageDecoder {

    override fun decode(context: Context, uri: Uri) = BitmapDecoder.from(context, uri)
        .config(Bitmap.Config.RGB_565)
        .useBuiltInDecoder()
        .mutable(false)
        .decode()
        ?: throw IllegalStateException("decoded bitmap is null")
}
