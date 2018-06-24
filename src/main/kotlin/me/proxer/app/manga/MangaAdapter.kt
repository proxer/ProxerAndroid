package me.proxer.app.manga

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.MainApplication.Companion.LOGGING_TAG
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.manga.MangaAdapter.ViewHolder
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.entity.manga.Page
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.getStackTraceString
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
            else -> savedInstanceState.getParcelable(REQUIRES_FALLBACK_STATE)
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

        newData.forEach { item ->
            val target = object : SimpleTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) = Unit
            }

            preloadTargets += target

            glide
                ?.downloadOnly()
                ?.load(ProxerUrls.mangaPageImage(server, entryId, id, item.decodedName).toString())
                ?.into(target)
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val shortAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_shortAnimTime)

        internal val image: SubsamplingScaleImageView by bindView(R.id.image)
        internal val errorIndicator: ImageView by bindView(R.id.errorIndicator)

        internal var glideTarget: GlideFileTarget? = null

        init {
            image.setDoubleTapZoomDuration(shortAnimationTime)

            errorIndicator.setIconicsImage(CommunityMaterial.Icon.cmd_refresh, 64)

            initListeners(itemView)
        }

        fun bind(item: Page) {
            image.setMinimumTileDpi(120)

            if (isVertical) {
                val width = DeviceUtils.getScreenWidth(image.context)
                val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()

                itemView.layoutParams.height = height
            } else {
                itemView.layoutParams.height = MATCH_PARENT
            }

            // Do not use RapidDecoder on Android M. Crashing when zooming on that specific version.
            val shouldUseRapidDecoder = (Build.VERSION.SDK_INT != Build.VERSION_CODES.M && item.name.endsWith("png"))
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
        private fun initListeners(itemView: View) {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    bind(data[it])
                }
            }

            @Suppress("LabeledExpression")
            image.setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                override fun onImageLoaded() {
                    image.setDoubleTapZoomScale(image.scale * 2.5f)
                    image.maxScale = image.scale * 2.5f
                }

                override fun onTileLoadError(error: Exception) = withSafeAdapterPosition(this@ViewHolder) {
                    handleImageLoadError(error, it)
                }

                override fun onImageLoadError(error: Exception) = withSafeAdapterPosition(this@ViewHolder) {
                    handleImageLoadError(error, it)
                }

                override fun onPreviewLoadError(error: Exception) = withSafeAdapterPosition(this@ViewHolder) {
                    handleImageLoadError(error, it)
                }
            })

            image.setOnTouchListener { view, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    val (viewX, viewY) = IntArray(2).apply { view.getLocationInWindow(this) }

                    lastTouchCoordinates = event.x + viewX to event.y + viewY
                }

                false
            }

            image.setOnClickListener {
                withSafeAdapterPosition(this) {
                    lastTouchCoordinates?.let { touchCoordinates ->
                        clickSubject.onNext(Triple(image, touchCoordinates, it))
                    }
                }
            }
        }

        private fun handleImageLoadError(error: Exception, position: Int) {
            // This happens on certain devices with certain images due to a buggy Skia library version.
            // Fallback to the less efficient, but working RapidDecoder in that case. If the RapidDecoder is already in
            // use, show the error indicator.
            if (requiresFallback[data[position].decodedName] == true) {
                errorIndicator.visibility = View.VISIBLE
                image.visibility = View.GONE

                Log.e(LOGGING_TAG, error.getStackTraceString())
            } else {
                requiresFallback.put(data[position].decodedName, true)

                bind(data[position])
            }
        }

        internal inner class GlideFileTarget : SimpleTarget<File>() {

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
}
