package me.proxer.app.ui.view.bbcode.prototype

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.autoDisposable
import de.number42.subsampling_pdf_decoder.PDFDecoder
import de.number42.subsampling_pdf_decoder.PDFRegionDecoder
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.extension.events
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.app.util.rx.SubsamplingScaleImageViewEventObservable
import me.proxer.app.util.wrapper.OriginalSizeGlideTarget
import me.proxer.app.util.wrapper.SimpleGlideRequestListener
import okhttp3.HttpUrl
import java.io.File

/**
 * @author Ruben Gees
 */
object PdfPrototype : AutoClosingPrototype {

    private const val WIDTH_ARGUMENT = "width"

    private val widthAttributeRegex = Regex("(?:size)? *= *(.+?)( |\$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *pdf *=? *\"?.*?\"?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *pdf *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val width = BBUtils.cutAttribute(code, widthAttributeRegex)?.toIntOrNull()

        return BBTree(this, parent, args = BBArgs(custom = arrayOf(WIDTH_ARGUMENT to width)))
    }

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(parent, args) }

        return when {
            childViews.isEmpty() -> childViews
            else -> listOf(
                SubsamplingScaleImageView(parent.context).also { view: SubsamplingScaleImageView ->
                    val url = (childViews.firstOrNull() as? TextView)?.text.toString().trim()
                    val parsedUrl = url.toPrefixedUrlOrNull()

                    @Suppress("UNCHECKED_CAST")
                    val heightMap = args[ImagePrototype.HEIGHT_MAP_ARGUMENT] as MutableMap<String, Int>?

                    val width = args[WIDTH_ARGUMENT] as Int? ?: MATCH_PARENT
                    val height = parsedUrl?.let { heightMap?.get(it.toString()) } ?: WRAP_CONTENT

                    view.layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, height)

                    val animationDuration = view.context.resources.getInteger(android.R.integer.config_shortAnimTime)

                    view.setDoubleTapZoomDuration(animationDuration)
                    view.setMinimumTileDpi(196)

                    args.glide?.let { loadImage(it, view, parsedUrl, heightMap) }

                    view.clicks()
                        .filter { view.getTag(R.id.error_tag) == true }
                        .autoDisposable(ViewScopeProvider.from(parent))
                        .subscribe {
                            view.tag = null

                            args.glide?.let { loadImage(it, view, parsedUrl, heightMap) }
                        }

                    view.doOnLayout {
                        if (width < view.width) {
                            view.updateLayoutParams {
                                this.width = width
                            }

                            view.requestLayout()
                        }
                    }
                }
            )
        }
    }

    private fun loadImage(
        glide: GlideRequests,
        view: SubsamplingScaleImageView,
        url: HttpUrl?,
        heightMap: MutableMap<String, Int>?
    ) = glide
        .download(url.toString())
        .listener(
            object : SimpleGlideRequestListener<File?> {
                override fun onResourceReady(
                    resource: File?,
                    model: Any?,
                    target: Target<File?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    (target as? GlidePdfTarget)?.view?.also { view ->
                        if (view.layoutParams.height <= 0) {
                            findHost(view)?.heightChanges?.onNext(Unit)
                        }
                    }

                    return false
                }
            }
        )
        .into(GlidePdfTarget(view, url.toString(), heightMap))

    private fun handleLoadError(view: SubsamplingScaleImageView) {
        view.setTag(R.id.error_tag, true)

        view.setImage(
            ImageSource.bitmap(
                IconicsDrawable(view.context, CommunityMaterial.Icon3.cmd_refresh).apply {
                    colorInt = view.context.resolveColor(R.attr.colorIcon)
                    sizeDp = 32
                }.toBitmap()
            )
        )
    }

    private fun findHost(view: View): BBCodeView? {
        var current = view.parent

        while (current !is BBCodeView && current is ViewGroup) {
            current = current.parent
        }

        return current as? BBCodeView
    }

    private class GlidePdfTarget(
        view: SubsamplingScaleImageView,
        private val model: String,
        private val dimensionMap: MutableMap<String, Int>?
    ) : OriginalSizeGlideTarget<File>() {

        var view: SubsamplingScaleImageView? = view
            private set

        private var regionDecoder: PDFRegionDecoder? = null

        init {
            view.doOnLayout {
                view.events()
                    .publish()
                    .autoConnect(2)
                    .also { observable ->
                        observable.filter { it is SubsamplingScaleImageViewEventObservable.Event.Error }
                            .autoDisposable(ViewScopeProvider.from(view))
                            .subscribe { handleLoadError(view) }

                        observable.filter { it is SubsamplingScaleImageViewEventObservable.Event.Loaded }
                            .autoDisposable(ViewScopeProvider.from(view))
                            .subscribe {
                                view.setDoubleTapZoomScale(view.scale * 2.5f)
                                view.maxScale = view.scale * 2.5f

                                dimensionMap?.put(model, view.height)
                            }
                    }
            }
        }

        override fun onResourceReady(resource: File, transition: Transition<in File>?) {
            view?.also { safeView ->
                regionDecoder = PDFRegionDecoder(0, resource, 8f).also {
                    safeView.setRegionDecoderFactory { it }
                }

                safeView.setBitmapDecoderFactory { PDFDecoder(0, resource, 8f) }

                safeView.setImage(ImageSource.uri(resource.absolutePath))
            }
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            view?.also { handleLoadError(it) }
        }

        override fun onLoadCleared(placeholder: Drawable?) {
            regionDecoder?.recycle()
            regionDecoder = null

            view = null
        }
    }
}
