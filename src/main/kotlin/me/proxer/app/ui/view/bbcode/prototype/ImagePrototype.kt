package me.proxer.app.ui.view.bbcode.prototype

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import com.bumptech.glide.load.engine.GlideException
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.ui.ImageDetailActivity
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.wrapper.SimpleGlideRequestListener
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object ImagePrototype : AutoClosingPrototype {

    private const val WIDTH_ARGUMENT = "width"

    private val WIDTH_ATTRIBUTE_REGEX = Regex("size *= *(.+?)( |$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *img( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *img *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val width = BBUtils.cutAttribute(code, WIDTH_ATTRIBUTE_REGEX)?.toIntOrNull()

        return BBTree(this, parent, args = BBArgs(custom = *arrayOf(WIDTH_ARGUMENT to width)))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(context, args) }

        val url = (childViews.firstOrNull() as? TextView)?.text.toString().trim()
        val parsedUrl = Utils.safelyParseAndFixUrl(url)

        val width = if (parsedUrl == null) null else args[WIDTH_ARGUMENT] as Int?

        return listOf(AppCompatImageView(context).also { view: ImageView ->
            ViewCompat.setTransitionName(view, "bb_image_$parsedUrl")

            view.layoutParams = LayoutParams(width ?: MATCH_PARENT, WRAP_CONTENT)

            args.glide?.let { loadImage(it, view, parsedUrl) }

            if (context is Activity) {
                view.setOnClickListener { _ ->
                    if (view.getTag(R.id.error_tag) == true) {
                        view.tag = null

                        args.glide?.let { loadImage(it, view, parsedUrl) }
                    } else if (view.drawable != null && parsedUrl != null) {
                        ImageDetailActivity.navigateTo(context, parsedUrl, view)
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
        .error(IconicsDrawable(view.context, CommunityMaterial.Icon.cmd_refresh)
            .iconColor(view.context)
            .sizeDp(32))
        .into(view)
}
