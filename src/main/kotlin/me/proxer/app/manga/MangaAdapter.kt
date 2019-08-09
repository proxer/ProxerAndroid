package me.proxer.app.manga

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.PAN_LIMIT_INSIDE
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.ZOOM_FOCUS_CENTER
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.uber.autodispose.autoDisposable
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
import me.proxer.app.util.GLUtil
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.events
import me.proxer.app.util.extension.logErrors
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.touchesMonitored
import me.proxer.app.util.rx.SubsamplingScaleImageViewEventObservable
import me.proxer.app.util.wrapper.OriginalSizeGlideTarget
import me.proxer.library.entity.manga.Chapter
import me.proxer.library.entity.manga.Page
import me.proxer.library.util.ProxerUrls
import timber.log.Timber
import java.io.File
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaAdapter(var isVertical: Boolean) : BaseAdapter<Page, MangaViewHolder>() {

    private companion object {
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_GIF = 2
    }

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Triple<View, Pair<Float, Float>, Int>> = PublishSubject.create()
    val lowMemorySubject: PublishSubject<Unit> = PublishSubject.create()

    var server by Delegates.notNull<String>()
    var entryId by Delegates.notNull<String>()
    var id by Delegates.notNull<String>()

    private var lastTouchCoordinates: Pair<Float, Float>? = null

    init {
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

        else -> error("Unknown viewType: $viewType")
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) = holder.bind(data[position])

    override fun getItemViewType(position: Int): Int {
        return when (data[position].decodedName.endsWith(".gif", ignoreCase = true)) {
            true -> VIEW_TYPE_GIF
            false -> VIEW_TYPE_IMAGE
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
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
            else -> error("Unknown ViewHolder: ${holder::class.java.name}")
        }
    }

    fun setChapter(chapter: Chapter) {
        this.server = chapter.server
        this.entryId = chapter.entryId
        this.id = chapter.id
    }

    abstract inner class MangaViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal abstract val image: View

        internal val errorIndicator: ImageView by bindView(R.id.errorIndicator)

        private val screenWidth = DeviceUtils.getScreenWidth(itemView.context)

        init {
            errorIndicator.setIconicsImage(CommunityMaterial.Icon2.cmd_refresh, 64)
        }

        open fun bind(item: Page) {
            initListeners()

            if (isVertical) {
                val height = (item.height * screenWidth.toFloat() / item.width.toFloat()).toInt()

                itemView.layoutParams.height = height
            } else {
                itemView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }

            errorIndicator.isVisible = false
            image.isVisible = true
        }

        protected fun handleImageLoadError(error: Exception) {
            Timber.e(error)

            when {
                error is OutOfMemoryError || error.cause is OutOfMemoryError -> lowMemorySubject.onNext(Unit)
                else -> {
                    errorIndicator.isVisible = true
                    image.isVisible = false
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
            image.setDoubleTapZoomStyle(ZOOM_FOCUS_CENTER)
            image.setMaxTileSize(GLUtil.maxTextureSize)
            image.setPanLimit(PAN_LIMIT_INSIDE)
            image.setMinimumTileDpi(196)
            image.setMinimumDpi(90)

            image.isExifInterfaceEnabled = false
        }

        override fun bind(item: Page) {
            super.bind(item)

            initListeners()

            image.setBitmapDecoderFactory { ImageLibDecoder() }
            image.setRegionDecoderFactory { ImageLibRegionDecoder() }

            glide?.clear(glideTarget)
            glideTarget = GlideFileTarget()

            glideTarget?.let { target ->
                glide
                    ?.downloadOnly()
                    ?.load(item.url())
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
                        .autoDisposable(this)
                        .subscribe { event -> handleImageLoadError(event.error) }

                    observable.filter { it is SubsamplingScaleImageViewEventObservable.Event.Ready }
                        .autoDisposable(this)
                        .subscribe {
                            val newMaxScale = image.minScale * 2.5f

                            image.setDoubleTapZoomScale(newMaxScale)
                            image.maxScale = newMaxScale

                            image.setScaleAndCenter(image.scale, image.center?.also { it.y = 0f })
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
                ?.load(item.url())
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

    private fun Page.url() = ProxerUrls.mangaPageImage(server, entryId, id, decodedName).toString()
}
