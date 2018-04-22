package me.proxer.app.manga

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import com.bumptech.glide.request.target.SimpleTarget
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
    val clickSubject: PublishSubject<Pair<View, Int>> = PublishSubject.create()

    var server by Delegates.notNull<String>()
    var entryId by Delegates.notNull<String>()
    var id by Delegates.notNull<String>()

    private val requiresFallback: ParcelableStringBooleanMap

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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
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

            @Suppress("LabeledExpression")
            image.setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
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

            errorIndicator.setIconicsImage(CommunityMaterial.Icon.cmd_refresh, 64)

            itemView.setOnClickListener {
                withSafeAdapterPosition(this, {
                    bind(data[it])
                })
            }

            image.setOnClickListener {
                withSafeAdapterPosition(this, {
                    clickSubject.onNext(image to it)
                })
            }
        }

        fun bind(item: Page) {
            val width = DeviceUtils.getScreenWidth(image.context)
            val scale = width.toFloat() / item.width.toFloat() * 2f

            image.setDoubleTapZoomScale(scale)
            image.maxScale = scale

            if (isVertical) {
                val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()

                itemView.layoutParams.height = height
            } else {
                itemView.layoutParams.height = MATCH_PARENT
            }

            if (item.name.endsWith("png") || requiresFallback[item.decodedName] == true) {
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
                    ?.download(ProxerUrls.mangaPageImage(server, entryId, id, item.decodedName).toString())
                    ?.into(target)
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
