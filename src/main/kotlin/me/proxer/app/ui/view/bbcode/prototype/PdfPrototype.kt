package me.proxer.app.ui.view.bbcode.prototype

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import de.number42.subsampling_pdf_decoder.PDFDecoder
import de.number42.subsampling_pdf_decoder.PDFRegionDecoder
import me.proxer.app.GlideRequests
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.iconColor
import okhttp3.HttpUrl
import org.jetbrains.anko.longToast
import java.io.File
import java.lang.Exception

/**
 * @author Ruben Gees
 */
object PdfPrototype : ConditionalTextMutatorPrototype, AutoClosingPrototype {

    private val WIDTH_ATTRIBUTE_REGEX = Regex("size *= *(.+?)( |$)", REGEX_OPTIONS)
    private const val WIDTH_ARGUMENT = "width"

    override val startRegex = Regex(" *pdf( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *pdf *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val width = BBUtils.cutAttribute(code, WIDTH_ATTRIBUTE_REGEX)?.toIntOrNull()

        return BBTree(this, parent, args = BBArgs(custom = *arrayOf(WIDTH_ARGUMENT to width)))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(context, args) }

        return when {
            childViews.isEmpty() -> childViews
            Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP -> applyToViews<TextView>(childViews, {
                it.text = mutate(it.text.toSpannableStringBuilder(), args)
            })
            else -> listOf(SubsamplingScaleImageView(context).also { view: SubsamplingScaleImageView ->
                val url = (childViews.firstOrNull() as? TextView)?.text.toString().trim()
                val parsedUrl = Utils.safelyParseAndFixUrl(url)

                val width = if (parsedUrl == null) null else args[WIDTH_ARGUMENT] as Int?

                view.layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                view.setMinimumTileDpi(120)
                view.setDoubleTapZoomDuration(view.context.resources.getInteger(android.R.integer.config_shortAnimTime))

                args.glide?.let { loadImage(it, view, parsedUrl) }

                if (context is Activity) {
                    view.setOnClickListener { _ ->
                        if (view.getTag(R.id.error_tag) == true) {
                            view.tag = null

                            args.glide?.let { loadImage(it, view, parsedUrl) }
                        }
                    }
                }

                view.post {
                    if (width != null && width < view.width) {
                        view.layoutParams.width = width

                        view.requestLayout()
                    }
                }
            })
        }
    }

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val uri = Uri.parse(text.toString())

        return text.toSpannableStringBuilder().apply {
            replace(0, length, globalContext.getString(R.string.view_bbcode_pdf_link))

            setSpan(UriClickableSpan(uri), 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    override fun canOptimize(recursiveChildren: List<BBTree>): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && super.canOptimize(recursiveChildren)
    }

    private fun loadImage(glide: GlideRequests, view: SubsamplingScaleImageView, url: HttpUrl?) = glide
        .download(url.toString())
        .into(object : SimpleTarget<File>() {
            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                view.setBitmapDecoderFactory { PDFDecoder(0, resource, 8f) }
                view.setRegionDecoderFactory { PDFRegionDecoder(0, resource, 8f) }

                view.setImage(ImageSource.uri(resource.absolutePath))

                view.setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                    override fun onImageLoaded() {
                        view.setDoubleTapZoomScale(view.scale * 2.5f)
                        view.maxScale = view.scale * 2.5f
                    }

                    override fun onTileLoadError(error: Exception) {
                        handleLoadError(view)
                    }

                    override fun onImageLoadError(error: Exception) {
                        handleLoadError(view)
                    }

                    override fun onPreviewLoadError(error: Exception) {
                        handleLoadError(view)
                    }
                })
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                handleLoadError(view)
            }
        })

    private fun handleLoadError(view: SubsamplingScaleImageView) {
        view.setTag(R.id.error_tag, true)

        view.setImage(ImageSource.bitmap(IconicsDrawable(view.context, CommunityMaterial.Icon.cmd_refresh)
            .iconColor(view.context)
            .sizeDp(32)
            .toBitmap()))
    }

    private class UriClickableSpan(private val uri: Uri) : ClickableSpan() {

        override fun onClick(widget: View) {
            try {
                widget.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (error: ActivityNotFoundException) {
                widget.context.longToast(widget.context.getString(R.string.view_bbcode_pdf_no_activity_error))
            }
        }
    }
}
