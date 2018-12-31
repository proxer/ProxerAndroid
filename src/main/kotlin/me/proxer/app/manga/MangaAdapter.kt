package me.proxer.app.manga

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.uber.autodispose.autoDisposable
import events
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.manga.MangaAdapter.MangaViewHolder
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.getSafeParcelable
import me.proxer.app.util.extension.logErrors
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.rx.SubsamplingScaleImageViewEventObservable
import me.proxer.app.util.wrapper.OriginalSizeGlideTarget
import me.proxer.library.entity.manga.Page
import me.proxer.library.util.ProxerUrls
import timber.log.Timber
import touchesMonitored
import java.io.File
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaAdapter(savedInstanceState: Bundle?, var isVertical: Boolean) : BaseAdapter<Page, MangaViewHolder>() {

    private companion object {
        private const val REQUIRES_FALLBACK_STATE = "manga_requires_fallback_state"
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_GIF = 2
    }

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Triple<View, Pair<Float, Float>, Int>> = PublishSubject.create()
    val lowMemorySubject: PublishSubject<Unit> = PublishSubject.create()

    var server by Delegates.notNull<String>()
    var entryId by Delegates.notNull<String>()
    var id by Delegates.notNull<String>()

    private val requiresFallback: ParcelableStringBooleanMap
    private val preloadTargets = mutableListOf<Target<File>>()

    private var lastTouchCoordinates: Pair<Float, Float>? = null

    init {
        requiresFallback = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getSafeParcelable(REQUIRES_FALLBACK_STATE)
        }

        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = data[position].decodedName.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_IMAGE -> ImageViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_manga_page, parent, false)
        )

        VIEW_TYPE_GIF -> GifViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_manga_page_gif, parent, false)
        )

        else -> throw IllegalArgumentException("Unknown viewType: $viewType")
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) = holder.bind(data[position])

    override fun getItemViewType(position: Int): Int {
        return when (data[position].decodedName.endsWith(".gif")) {
            true -> VIEW_TYPE_GIF
            false -> VIEW_TYPE_IMAGE
        }
    }

    override fun swapDataAndNotifyWithDiffing(newData: List<Page>) {
        super.swapDataAndNotifyWithDiffing(newData)

        preloadTargets.forEach { glide?.clear(it) }
        preloadTargets.clear()

        val preloadList = newData.map {
            ProxerUrls.mangaPageImage(server, entryId, id, it.decodedName).toString()
        }

        if (preloadList.isNotEmpty()) {
            val preloadMap = preloadList
                .asSequence()
                .mapIndexed { index, url -> url to preloadList.getOrNull(index + 1) }
                .associate { it }

            preload(preloadMap, preloadList.first())
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        preloadTargets.forEach { glide?.clear(it) }
        preloadTargets.clear()

        glide = null
    }

    override fun onViewRecycled(holder: MangaViewHolder) {
        when (holder) {
            is ImageViewHolder -> {
                glide?.clear(holder.glideTarget)

                holder.glideTarget = null
                holder.image.recycle()
            }
            is GifViewHolder -> glide?.clear(holder.image)
            else -> throw IllegalArgumentException("Unknown ViewHolder: ${holder::class.java.name}")
        }
    }

    override fun saveInstanceState(outState: Bundle) = outState.putParcelable(REQUIRES_FALLBACK_STATE, requiresFallback)

    private fun preload(links: Map<String, String?>, next: String, failures: Int = 0) {
        val target = GlidePreloadTarget(links, next, failures)

        preloadTargets += target

        glide
            ?.downloadOnly()
            ?.load(next)
            ?.logErrors()
            ?.into(target)
    }

    abstract inner class MangaViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal abstract val image: View

        internal val errorIndicator: ImageView by bindView(R.id.errorIndicator)

        init {
            errorIndicator.setIconicsImage(CommunityMaterial.Icon2.cmd_refresh, 64)
        }

        open fun bind(item: Page) {
            initListeners()

            if (isVertical) {
                val width = DeviceUtils.getScreenWidth(image.context)
                val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()

                itemView.layoutParams.height = height
            } else {
                itemView.layoutParams.height = MATCH_PARENT
            }

            errorIndicator.isVisible = false
            image.isVisible = true
        }

        protected fun handleImageLoadError(error: Exception, position: Int) {
            // This happens on certain devices with certain images due to a buggy Skia library version.
            // Fallback to the less efficient, but working RapidDecoder in that case. If the RapidDecoder is already in
            // use, show the error indicator.
            Timber.e(error)

            when {
                error is OutOfMemoryError -> lowMemorySubject.onNext(Unit)
                requiresFallback[data[position].decodedName] == true -> {
                    errorIndicator.isVisible = true
                    image.isVisible = false
                }
                else -> {
                    requiresFallback.put(data[position].decodedName, true)

                    bind(data[position])
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun initListeners() {
            // Rebind on itemView clicks.
            // This only emits if the image is not visible, which is the case if an error occurred.
            itemView.clicks()
                .mapAdapterPosition({ positionResolver.resolve(adapterPosition) }) { data[it] }
                .autoDisposable(this)
                .subscribe(this::bind)

            image.touchesMonitored { false }
                .filter { it.actionMasked == MotionEvent.ACTION_DOWN }
                .map {
                    val (viewX, viewY) = IntArray(2).apply { image.getLocationInWindow(this) }

                    it.x + viewX to it.y + viewY
                }
                .autoDisposable(this)
                .subscribe { lastTouchCoordinates = it }

            image.clicks()
                .mapAdapterPosition({ adapterPosition }) { positionResolver.resolve(it) }
                .flatMap { position ->
                    Observable.just(lastTouchCoordinates.toOptional())
                        .filterSome()
                        .map { Triple(image, it, position) }
                }
                .autoDisposable(this)
                .subscribe(clickSubject)
        }
    }

    inner class ImageViewHolder(itemView: View) : MangaViewHolder(itemView) {

        override val image: SubsamplingScaleImageView by bindView(R.id.image)

        internal var glideTarget: GlideFileTarget? = null

        private val shortAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_shortAnimTime)

        init {
            image.setDoubleTapZoomDuration(shortAnimationTime)
            image.isExifInterfaceEnabled = false
        }

        override fun bind(item: Page) {
            super.bind(item)

            initListeners()

            image.setMinimumTileDpi(120)

            val useRapidDecoder = requiresFallback[item.decodedName] == true

            if (useRapidDecoder) {
                image.setBitmapDecoderClass(RapidImageDecoder::class.java)
                image.setRegionDecoderClass(RapidImageRegionDecoder::class.java)
            } else {
                image.setBitmapDecoderClass(SkiaImageDecoder::class.java)
                image.setRegionDecoderClass(SkiaPooledImageRegionDecoder::class.java)
            }

            glide?.clear(glideTarget)
            glideTarget = GlideFileTarget()

            glideTarget?.let { target ->
                glide
                    ?.downloadOnly()
                    ?.load(ProxerUrls.mangaPageImage(server, entryId, id, item.decodedName).toString())
                    ?.logErrors()
                    ?.into(target)
            }
        }

        private fun initListeners() {
            image.events()
                .publish()
                .also { observable ->
                    observable.filter { it is SubsamplingScaleImageViewEventObservable.Event.Error }
                        .map { it as SubsamplingScaleImageViewEventObservable.Event.Error }
                        .flatMap { event ->
                            Observable.just(Unit)
                                .mapAdapterPosition({ adapterPosition }) { event.error to positionResolver.resolve(it) }
                        }
                        .autoDisposable(this)
                        .subscribe { (error, position) -> handleImageLoadError(error, position) }

                    observable.filter { it is SubsamplingScaleImageViewEventObservable.Event.Ready }
                        .autoDisposable(this)
                        .subscribe {
                            val newMaxScale = image.minScale * 2.5f

                            image.setDoubleTapZoomScale(newMaxScale)
                            image.maxScale = newMaxScale

                            image.resetScaleAndCenter()
                        }
                }
                .connect()
        }

        internal inner class GlideFileTarget : OriginalSizeGlideTarget<File>() {

            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                Single.fromCallable { ImageSource.uri(resource.path) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeAndLogErrors { source -> image.setImage(source) }
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                errorIndicator.isVisible = true
                image.isVisible = false
            }
        }
    }

    inner class GifViewHolder(itemView: View) : MangaViewHolder(itemView) {

        override val image: ImageView by bindView(R.id.image)

        override fun bind(item: Page) {
            super.bind(item)

            glide
                ?.asGif()
                ?.load(ProxerUrls.mangaPageImage(server, entryId, id, item.decodedName).toString())
                ?.logErrors()
                ?.into(object : ImageViewTarget<GifDrawable>(image) {
                    override fun setResource(resource: GifDrawable?) {
                        view.setImageDrawable(resource)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        errorIndicator.isVisible = true
                        image.isVisible = false
                    }
                })
        }
    }

    internal inner class GlidePreloadTarget(
        private val links: Map<String, String?>,
        private val next: String,
        private val failures: Int
    ) : OriginalSizeGlideTarget<File>() {

        override fun onResourceReady(resource: File, transition: Transition<in File>?) {
            val afterNext = links[next]

            if (afterNext != null) {
                preload(links, afterNext)
            }
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            if (failures <= 2) {
                preload(links, next, failures + 1)
            }
        }
    }
}
