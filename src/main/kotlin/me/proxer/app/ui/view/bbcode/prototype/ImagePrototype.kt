package me.proxer.app.ui.view.bbcode.prototype

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.autoDisposable
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.ui.ImageDetailActivity
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.extension.logErrors
import me.proxer.app.util.extension.proxyIfRequired
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.app.util.wrapper.SimpleGlideRequestListener
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object ImagePrototype : AutoClosingPrototype {

    const val HEIGHT_MAP_ARGUMENT = "dimension_map"

    private const val WIDTH_ARGUMENT = "width"

    private val widthAttributeRegex = Regex("(?:size)? *= *(.+?)( |$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *img *=? *\"?.*?\"?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *img *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val width = BBUtils.cutAttribute(code, widthAttributeRegex)?.toIntOrNull()

        return BBTree(this, parent, args = BBArgs(custom = arrayOf(WIDTH_ARGUMENT to width)))
    }

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(parent, args) }

        val url = (childViews.firstOrNull() as? TextView)?.text.toString().trim()
        val proxyUrl = url.toPrefixedUrlOrNull()?.proxyIfRequired()

        @Suppress("UNCHECKED_CAST")
        val heightMap = args[HEIGHT_MAP_ARGUMENT] as MutableMap<String, Int>?

        val width = args[WIDTH_ARGUMENT] as Int? ?: MATCH_PARENT
        val height = proxyUrl?.let { heightMap?.get(it.toString()) } ?: WRAP_CONTENT

        return listOf(
            AppCompatImageView(parent.context).also { view: ImageView ->
                ViewCompat.setTransitionName(view, "bb_image_$proxyUrl")

                view.layoutParams = ViewGroup.MarginLayoutParams(width, height)

                args.glide?.let { loadImage(it, view, proxyUrl, heightMap) }

                (parent.context as? Activity)?.let { context ->
                    view.clicks()
                        .autoDisposable(ViewScopeProvider.from(parent))
                        .subscribe {
                            if (view.getTag(R.id.error_tag) == true) {
                                view.tag = null

                                args.glide?.let { loadImage(it, view, proxyUrl, heightMap) }
                            } else if (view.drawable != null && proxyUrl != null) {
                                ImageDetailActivity.navigateTo(context, proxyUrl, view)
                            }
                        }
                }
            }
        )
    }

    private fun loadImage(
        glide: GlideRequests,
        view: ImageView,
        url: HttpUrl?,
        heightMap: MutableMap<String, Int>?
    ) = glide
        .load(url.toString())
        .centerInside()
        .listener(
            object : SimpleGlideRequestListener<Drawable?> {
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (resource is Drawable && model is String) {
                        heightMap?.put(model, resource.intrinsicHeight)
                    }

                    if (target is ImageViewTarget && target.view.layoutParams.height <= 0) {
                        findHost(target.view)?.heightChanges?.onNext(Unit)
                    }

                    return false
                }

                override fun onLoadFailed(error: GlideException?): Boolean {
                    view.setTag(R.id.error_tag, true)

                    return false
                }
            }
        )
        .error(
            IconicsDrawable(view.context, CommunityMaterial.Icon3.cmd_refresh).apply {
                colorInt = view.context.resolveColor(R.attr.colorIcon)
                sizeDp = 32
            }
        )
        .logErrors()
        .into(view)

    private fun findHost(view: View): BBCodeView? {
        var current = view.parent

        while (current !is BBCodeView && current is ViewGroup) {
            current = current.parent
        }

        return current as? BBCodeView
    }
}
