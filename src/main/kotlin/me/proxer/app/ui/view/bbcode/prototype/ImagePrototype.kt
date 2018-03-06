package me.proxer.app.ui.view.bbcode.prototype

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.TextView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.ui.ImageDetailActivity
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBTree.Companion.GLIDE_ARGUMENT
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.iconColor
import okhttp3.HttpUrl
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
object ImagePrototype : AutoClosingPrototype {

    private val WIDTH_ATTRIBUTE_REGEX = Regex("size *= *(.+?)( |$)", REGEX_OPTIONS)
    private const val WIDTH_ARGUMENT = "width"

    private val INVALID_IMAGE = HttpUrl.parse("https://cdn.proxer.me/keinbild.jpg")
            ?: throw IllegalArgumentException("Could not parse url")

    override val startRegex = Regex(" *img( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *img *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val width = BBUtils.cutAttribute(code, WIDTH_ATTRIBUTE_REGEX)?.toIntOrNull()

        return BBTree(this, parent, args = mutableMapOf(WIDTH_ARGUMENT to width))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }

        val url = (childViews.firstOrNull() as? TextView)?.text.toString().trim()
        val parsedUrl = Utils.safelyParseAndFixUrl(url) ?: INVALID_IMAGE

        val glide = args[GLIDE_ARGUMENT] as GlideRequests?
        val width = if (parsedUrl == INVALID_IMAGE) context.dip(100) else args[WIDTH_ARGUMENT] as Int?

        return listOf(AppCompatImageView(context).also { view: ImageView ->
            ViewCompat.setTransitionName(view, "bb_image_$parsedUrl")

            view.layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            glide?.let { loadImage(it, view, parsedUrl, width) }

            if (context is Activity) {
                view.setOnClickListener { _ ->
                    if (view.getTag(R.id.error_tag) == true) {
                        view.tag = null

                        glide?.let { loadImage(it, view, parsedUrl, width) }
                    } else if (view.drawable != null) {
                        ImageDetailActivity.navigateTo(context, parsedUrl, view)
                    }
                }
            }
        })
    }

    private fun loadImage(glide: GlideRequests, view: ImageView, url: HttpUrl, width: Int?) = glide.load(url.toString())
            .apply { if (width != null) override(width, SIZE_ORIGINAL) else override(SIZE_ORIGINAL) }
            .format(when (DeviceUtils.shouldShowHighQualityImages(view.context)) {
                true -> DecodeFormat.PREFER_ARGB_8888
                false -> DecodeFormat.PREFER_RGB_565
            })
            .listener(object : RequestListener<Drawable?> {

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    view.setTag(R.id.error_tag, true)

                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ) = false
            })
            .error(IconicsDrawable(view.context, CommunityMaterial.Icon.cmd_refresh)
                    .iconColor(view.context)
                    .sizeDp(32))
            .into(view)
}
