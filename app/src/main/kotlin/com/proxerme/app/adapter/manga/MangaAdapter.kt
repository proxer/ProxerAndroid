package com.proxerme.app.adapter.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.DeviceUtils
import com.proxerme.app.util.decodedName
import com.proxerme.library.connection.manga.entity.Page
import com.proxerme.library.info.ProxerUrlHolder
import java.io.File

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

        private var target: Target<File>? = null

        private val image: SubsamplingScaleImageView
            get() = itemView as SubsamplingScaleImageView

        override fun bind(item: Page) {
            val width = DeviceUtils.getScreenWidth(image.context)
            val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()

            image.recycle()
            image.layoutParams.height = height

            target?.let {
                Glide.clear(it)
            }

            target = object : SimpleTarget<File>() {
                override fun onResourceReady(resource: File,
                                             glideAnimation: GlideAnimation<in File>?) {
                    image.setImage(ImageSource.uri(resource.path))
                }
            }

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getMangaPageUrl(server!!, entryId!!, id!!,
                            item.decodedName).toString())
                    .downloadOnly(target)
        }
    }
}

