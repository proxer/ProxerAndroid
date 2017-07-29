package me.proxer.app.util.view

import android.graphics.*
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * Idea of implementation from here: https://github.com/wasabeef/glide-transformations
 *
 * @author Ruben Gees
 */
class GlideGrayscaleTransformation : BitmapTransformation() {

    companion object {
        private val ID = "me.proxer.app.util.view.GlideGrayscaleTransformation"
        private val ID_BYTES = ID.toByteArray(Key.CHARSET)

        private val saturation = ColorMatrix().apply {
            setSaturation(0f)
        }

        private val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(saturation)
        }
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int) = toTransform.apply {
        Canvas(this).drawBitmap(this, 0f, 0f, paint)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    override fun equals(other: Any?) = other is GlideGrayscaleTransformation
    override fun hashCode() = ID.hashCode()
}