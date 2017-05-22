package me.proxer.app.adapter.base

import android.widget.ImageView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
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

    protected fun loadImage(view: ImageView, url: HttpUrl, circleCrop: Boolean = false) {
        glide?.load(url.toString())
                ?.transition(DrawableTransitionOptions.withCrossFade())
                ?.apply {
                    if (circleCrop) {
                        circleCrop()
                    }
                }
                ?.into(view)
    }

    protected fun clearImage(view: ImageView) {
        glide?.clear(view)
    }
}