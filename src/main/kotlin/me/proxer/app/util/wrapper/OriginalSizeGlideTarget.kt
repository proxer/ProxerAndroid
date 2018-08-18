package me.proxer.app.util.wrapper

import android.graphics.drawable.Drawable
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target

/**
 * @author Ruben Gees
 */
abstract class OriginalSizeGlideTarget<R> : Target<R> {

    private var request: Request? = null

    override fun onLoadStarted(placeholder: Drawable?) = Unit
    override fun onLoadFailed(errorDrawable: Drawable?) = Unit
    override fun onLoadCleared(placeholder: Drawable?) = Unit

    override fun onStart() = Unit
    override fun onStop() = Unit
    override fun onDestroy() = Unit

    override fun removeCallback(cb: SizeReadyCallback) = Unit

    override fun getSize(cb: SizeReadyCallback) {
        cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
    }

    override fun getRequest(): Request? {
        return request
    }

    override fun setRequest(request: Request?) {
        this.request = request
    }
}
