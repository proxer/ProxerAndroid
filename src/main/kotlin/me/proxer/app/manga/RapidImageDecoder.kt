package me.proxer.app.manga

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder
import rapid.decoder.BitmapDecoder

/**
 * @author Ruben Gees
 */
class RapidImageDecoder : ImageDecoder {

    override fun decode(context: Context, uri: Uri) = BitmapDecoder.from(context, uri)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    config(Bitmap.Config.HARDWARE)
                } else {
                    config(Bitmap.Config.ARGB_8888)
                }
            }
            .decode()
}
