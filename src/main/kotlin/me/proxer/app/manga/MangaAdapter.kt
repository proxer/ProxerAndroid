package me.proxer.app.manga

import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.manga.MangaAdapter.ViewHolder
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.library.entity.manga.Page
import me.proxer.library.util.ProxerUrls
import java.io.File
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaAdapter(private val isVertical: Boolean) : BaseAdapter<Page, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Int> = PublishSubject.create()

    var server by Delegates.notNull<String>()
    var entryId by Delegates.notNull<String>()
    var id by Delegates.notNull<String>()

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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val shortAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_shortAnimTime)
        private val mediumAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_mediumAnimTime)

        internal val image: SubsamplingScaleImageView by bindView(R.id.image)
        internal val errorIndicator: ImageView by bindView(R.id.errorIndicator)

        internal var glideTarget: GlideFileTarget? = null

        init {
            image.setDoubleTapZoomDuration(shortAnimationTime)
            image.setBitmapDecoderClass(RapidImageDecoder::class.java)
            image.setRegionDecoderClass(RapidImageRegionDecoder::class.java)

            errorIndicator.setIconicsImage(CommunityMaterial.Icon.cmd_refresh, 64)

            itemView.setOnClickListener {
                withSafeAdapterPosition(this, {
                    bind(data[it])
                })
            }

            if (!isVertical) {
                itemView.layoutParams.height = MATCH_PARENT

                image.setOnClickListener {
                    withSafeAdapterPosition(this, {
                        clickSubject.onNext(it)
                    })
                }
            }
        }

        fun bind(item: Page) {
            if (isVertical) {
                val width = DeviceUtils.getScreenWidth(image.context)
                val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()
                val scale = width.toFloat() / item.width.toFloat() * 2f

                itemView.layoutParams.height = height

                image.setDoubleTapZoomScale(scale)
                image.maxScale = scale
            }

            errorIndicator.visibility = View.GONE
            image.visibility = View.VISIBLE

            glide?.clear(glideTarget)
            glideTarget = GlideFileTarget()

            glideTarget?.let { target ->
                glide?.download(ProxerUrls.mangaPageImage(server, entryId, id, item.decodedName).toString())
                    ?.format(when (DeviceUtils.shouldShowHighQualityImages(image.context)) {
                        true -> DecodeFormat.PREFER_ARGB_8888
                        false -> DecodeFormat.PREFER_RGB_565
                    })
                    ?.into(target)
            }
        }

        internal inner class GlideFileTarget : SimpleTarget<File>() {

            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                image.setImage(ImageSource.uri(resource.path))
                image.setScaleAndCenter(0.2f, PointF(0f, 0f))

                // Fade animations do not look good with the horizontal reader.
                if (isVertical) {
                    image.apply { alpha = 0.2f }
                        .animate()
                        .alpha(1.0f)
                        .setDuration(mediumAnimationTime.toLong())
                        .start()
                }
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                errorIndicator.visibility = View.VISIBLE
                image.visibility = View.GONE
            }
        }
    }
}
