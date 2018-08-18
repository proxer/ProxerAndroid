package me.proxer.app.manga

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.uber.autodispose.kotlin.autoDisposable
import events
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Predicate
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.manga.MangaAdapter.ViewHolder
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.getSafeParcelable
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
import java.lang.Exception
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaAdapter(savedInstanceState: Bundle?, var isVertical: Boolean) : BaseAdapter<Page, ViewHolder>() {

    private companion object {
        private const val REQUIRES_FALLBACK_STATE = "manga_requires_fallback_state"
    }

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Triple<View, Pair<Float, Float>, Int>> = PublishSubject.create()

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_manga_page, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun swapDataAndNotifyWithDiffing(newData: List<Page>) {
        super.swapDataAndNotifyWithDiffing(newData)

        preloadTargets.forEach { glide?.clear(it) }
        preloadTargets.clear()

        val preloadList = newData.map {
            ProxerUrls.mangaPageImage(server, entryId, id, it.decodedName).toString()
        }

        if (preloadList.isNotEmpty()) {
            val preloadMap = preloadList
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

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.glideTarget)

        holder.glideTarget = null
        holder.image.recycle()
    }

    override fun saveInstanceState(outState: Bundle) = outState.putParcelable(REQUIRES_FALLBACK_STATE, requiresFallback)

    private fun preload(links: Map<String, String?>, next: String) {
        val target = GlidePreloadTarget(links, next)

        preloadTargets += target

        glide
            ?.downloadOnly()
            ?.load(next)
            ?.into(target)
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        private val shortAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_shortAnimTime)

        internal val image: SubsamplingScaleImageView by bindView(R.id.image)
        internal val errorIndicator: ImageView by bindView(R.id.errorIndicator)

        internal var glideTarget: GlideFileTarget? = null

        init {
            image.setDoubleTapZoomDuration(shortAnimationTime)
            image.isExifInterfaceEnabled = false

            errorIndicator.setIconicsImage(CommunityMaterial.Icon.cmd_refresh, 64)
        }

        fun bind(item: Page) {
            initListeners()

            image.setMinimumTileDpi(120)

            if (isVertical) {
                val width = DeviceUtils.getScreenWidth(image.context)
                val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()

                itemView.layoutParams.height = height
            } else {
                itemView.layoutParams.height = MATCH_PARENT
            }

            // Do not use RapidDecoder on Android M. Crashing when zooming on that specific version.
            val shouldUseRapidDecoder = Build.VERSION.SDK_INT != Build.VERSION_CODES.M && item.name.endsWith("png")
            val mustUseRapidDecoder = requiresFallback[item.decodedName] == true

            if (shouldUseRapidDecoder || mustUseRapidDecoder) {
                image.setBitmapDecoderClass(RapidImageDecoder::class.java)
                image.setRegionDecoderClass(RapidImageRegionDecoder::class.java)
            } else {
                image.setBitmapDecoderClass(SkiaImageDecoder::class.java)
                image.setRegionDecoderClass(SkiaPooledImageRegionDecoder::class.java)
            }

            errorIndicator.visibility = View.GONE
            image.visibility = View.VISIBLE

            glide?.clear(glideTarget)
            glideTarget = GlideFileTarget()

            glideTarget?.let { target ->
                glide
                    ?.downloadOnly()
                    ?.load(ProxerUrls.mangaPageImage(server, entryId, id, item.decodedName).toString())
                    ?.into(target)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun initListeners() {
            itemView.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe(this::bind)

            image.events()
                .publish()
                .also { observable ->
                    observable.filter { it is SubsamplingScaleImageViewEventObservable.Event.Error }
                        .map { it as SubsamplingScaleImageViewEventObservable.Event.Error }
                        .flatMap { event ->
                            Observable.just(Unit).mapAdapterPosition({ adapterPosition }) { event.error to it }
                        }
                        .autoDisposable(this)
                        .subscribe { (error, position) -> handleImageLoadError(error, position) }

                    observable.filter { it is SubsamplingScaleImageViewEventObservable.Event.Loaded }
                        .autoDisposable(this)
                        .subscribe {
                            image.setDoubleTapZoomScale(image.scale * 2.5f)
                            image.maxScale = image.scale * 2.5f
                        }
                }
                .connect()

            image.touchesMonitored(Predicate { false })
                .filter { it.actionMasked == MotionEvent.ACTION_DOWN }
                .map {
                    val (viewX, viewY) = IntArray(2).apply { image.getLocationInWindow(this) }

                    it.x + viewX to it.y + viewY
                }
                .autoDisposable(this)
                .subscribe { lastTouchCoordinates = it }

            image.clicks()
                .mapAdapterPosition({ adapterPosition }) { it }
                .flatMap { position ->
                    Observable.just(lastTouchCoordinates.toOptional())
                        .filterSome()
                        .map { Triple(image as View, it, position) }
                }
                .autoDisposable(this)
                .subscribe(clickSubject)
        }

        private fun handleImageLoadError(error: Exception, position: Int) {
            // This happens on certain devices with certain images due to a buggy Skia library version.
            // Fallback to the less efficient, but working RapidDecoder in that case. If the RapidDecoder is already in
            // use, show the error indicator.
            if (requiresFallback[data[position].decodedName] == true) {
                errorIndicator.visibility = View.VISIBLE
                image.visibility = View.GONE

                Timber.e(error)
            } else {
                requiresFallback.put(data[position].decodedName, true)

                bind(data[position])
            }
        }

        internal inner class GlideFileTarget : OriginalSizeGlideTarget<File>() {

            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                Single.fromCallable { ImageSource.uri(resource.path) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeAndLogErrors { source ->
                        image.setImage(source)
                        image.setScaleAndCenter(image.minScale, image.center)
                    }
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                errorIndicator.visibility = View.VISIBLE
                image.visibility = View.GONE
            }
        }
    }

    internal inner class GlidePreloadTarget(
        private val links: Map<String, String?>,
        private val next: String
    ) : OriginalSizeGlideTarget<File>() {

        override fun onResourceReady(resource: File, transition: Transition<in File>?) {
            val afterNext = links[next]

            if (afterNext != null) {
                preload(links, afterNext)
            }
        }
    }
}
