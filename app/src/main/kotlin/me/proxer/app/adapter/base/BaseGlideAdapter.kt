package me.proxer.app.adapter.base

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import me.proxer.app.application.GlideRequest
import me.proxer.app.application.GlideRequests
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
abstract class BaseGlideAdapter<T>(glide: GlideRequests) : BaseAdapter<T>() {

    private var glide: GlideRequests? = glide

    override fun destroy() {
        super.destroy()

        glide = null
    }

    protected fun loadImage(view: ImageView, url: HttpUrl,
                            adjustments: ((GlideRequest<Drawable>) -> GlideRequest<Drawable>)? = null) {
        glide?.load(url.toString())
                ?.transition(DrawableTransitionOptions.withCrossFade())
                ?.let { adjustments?.invoke(it) ?: it }
                ?.into(view)
    }

    protected fun clearImage(view: ImageView) {
        glide?.clear(view)
    }
}