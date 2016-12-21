package com.proxerme.app.adapter.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.DeviceUtils
import com.proxerme.library.connection.manga.entity.Page
import com.proxerme.library.info.ProxerUrlHolder
import uk.co.senab.photoview.PhotoView

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MangaAdapter : PagingAdapter<Page>() {

    private var server: String? = null
    private var entryId: String? = null
    private var id: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<Page> {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_manga_page, parent, false))
    }

    fun init(server: String, entryId: String, id: String) {
        this.server = server
        this.entryId = entryId
        this.id = id
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<Page>(itemView) {

        private val image: PhotoView
            get() = itemView as PhotoView

        override fun bind(item: Page) {
            val width = DeviceUtils.getScreenWidth(image.context)
            val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()

            image.minimumHeight = height
            image.setImageBitmap(null)
            image.post {
                image.setScale(1.0f, false)
            }

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getMangaPageUrl(server!!, entryId!!, id!!, item.name)
                            .toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(object : SimpleTarget<GlideDrawable>() {
                        override fun onResourceReady(resource: GlideDrawable?,
                                                     glideAnimation: GlideAnimation<in GlideDrawable>?) {
                            image.setImageDrawable(resource)
                            image.alpha = 0.2f
                            image.animate().alpha(1.0f).start()
                        }
                    })
        }
    }
}