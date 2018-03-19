package me.proxer.app.manga

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder
import me.proxer.app.util.DeviceUtils
import rapid.decoder.BitmapDecoder

/**
 * @author Ruben Gees
 */
class RapidImageDecoder : ImageDecoder {

    override fun decode(context: Context, uri: Uri) = BitmapDecoder.from(context, uri)
        .apply {
            when (DeviceUtils.shouldShowHighQualityImages(context)) {
                true -> config(Bitmap.Config.ARGB_8888)
                false -> config(Bitmap.Config.RGB_565)
            }
        }
        .decode()
        ?: throw NullPointerException("decoded bitmap is null")
}
