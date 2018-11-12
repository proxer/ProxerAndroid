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
import com.bumptech.glide.load.engine.GlideException
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
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
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.logErrors
import me.proxer.app.util.wrapper.SimpleGlideRequestListener
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object ImagePrototype : AutoClosingPrototype {

    private const val WIDTH_ARGUMENT = "width"

    private val widthAttributeRegex = Regex("size *= *(.+?)( |$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *img( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *img *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val width = BBUtils.cutAttribute(code, widthAttributeRegex)?.toIntOrNull()

        return BBTree(this, parent, args = BBArgs(custom = *arrayOf(WIDTH_ARGUMENT to width)))
    }

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(parent, args) }

        val url = (childViews.firstOrNull() as? TextView)?.text.toString().trim()
        val proxyUrl = Utils.parseAndFixUrl(url)?.let { ProxerUrls.proxyImage(it) }

        val width = if (proxyUrl == null) null else args[WIDTH_ARGUMENT] as Int?

        return listOf(AppCompatImageView(parent.context).also { view: ImageView ->
            ViewCompat.setTransitionName(view, "bb_image_$proxyUrl")

            view.layoutParams = ViewGroup.MarginLayoutParams(width ?: MATCH_PARENT, WRAP_CONTENT)

            args.glide?.let { loadImage(it, view, proxyUrl) }

            (parent.context as? Activity)?.let { context ->
                view.clicks()
                    .autoDisposable(ViewScopeProvider.from(parent))
                    .subscribe {
                        if (view.getTag(R.id.error_tag) == true) {
                            view.tag = null

                            args.glide?.let { loadImage(it, view, proxyUrl) }
                        } else if (view.drawable != null && proxyUrl != null) {
                            ImageDetailActivity.navigateTo(context, proxyUrl, view)
                        }
                    }
            }
        })
    }

    private fun loadImage(glide: GlideRequests, view: ImageView, url: HttpUrl?) = glide
        .load(url.toString())
        .centerInside()
        .listener(object : SimpleGlideRequestListener<Drawable?> {
            override fun onLoadFailed(error: GlideException?): Boolean {
                view.setTag(R.id.error_tag, true)

                return false
            }
        })
        .error(
            IconicsDrawable(view.context, CommunityMaterial.Icon2.cmd_refresh)
                .iconColor(view.context)
                .sizeDp(32)
        )
        .logErrors()
        .into(view)
}
