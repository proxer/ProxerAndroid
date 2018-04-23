package me.proxer.app.util.wrapper

import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * @author Ruben Gees
 */
@Suppress("FunctionOnlyReturningConstant")
interface SimpleGlideRequestListener<R> : RequestListener<R> {

    override fun onLoadFailed(
        error: GlideException?,
        model: Any?,
        target: Target<R>?,
        isFirstResource: Boolean
    ) = onLoadFailed(error)

    override fun onResourceReady(
        resource: R,
        model: Any?,
        target: Target<R>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ) = onResourceReady(resource)

    fun onLoadFailed(error: GlideException?) = false
    fun onResourceReady(resource: R) = false
}
