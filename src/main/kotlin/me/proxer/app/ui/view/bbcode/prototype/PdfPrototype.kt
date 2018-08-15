package me.proxer.app.ui.view.bbcode.prototype

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import de.number42.subsampling_pdf_decoder.PDFDecoder
import de.number42.subsampling_pdf_decoder.PDFRegionDecoder
import events
import me.proxer.app.GlideRequests
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.rx.SubsamplingScaleImageViewEventObservable
import okhttp3.HttpUrl
import org.jetbrains.anko.longToast
import java.io.File

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

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(parent, args) }

        return when {
            childViews.isEmpty() -> childViews
            Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP -> applyToViews<TextView>(childViews) {
                it.text = mutate(it.text.toSpannableStringBuilder(), args)
            }
            else -> listOf(SubsamplingScaleImageView(parent.context).also { view: SubsamplingScaleImageView ->
                val url = (childViews.firstOrNull() as? TextView)?.text.toString().trim()
                val parsedUrl = Utils.parseAndFixUrl(url)

                val width = if (parsedUrl == null) null else args[WIDTH_ARGUMENT] as Int?

                view.layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)

                view.setMinimumTileDpi(120)
                view.setDoubleTapZoomDuration(view.context.resources.getInteger(android.R.integer.config_shortAnimTime))

                args.glide?.let { loadImage(it, view, parsedUrl) }

                view.clicks()
                    .filter { view.getTag(R.id.error_tag) == true }
                    .autoDisposable(ViewScopeProvider.from(parent))
                    .subscribe { _ ->
                        view.tag = null

                        args.glide?.let { loadImage(it, view, parsedUrl) }
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

                view.events()
                    .publish()
                    .also { observable ->
                        observable.filter { it is SubsamplingScaleImageViewEventObservable.Event.Error }
                            .autoDisposable(ViewScopeProvider.from(view))
                            .subscribe { handleLoadError(view) }

                        observable.filter { it is SubsamplingScaleImageViewEventObservable.Event.Loaded }
                            .autoDisposable(ViewScopeProvider.from(view))
                            .subscribe {
                                view.setDoubleTapZoomScale(view.scale * 2.5f)
                                view.maxScale = view.scale * 2.5f
                            }
                    }
                    .connect()
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
