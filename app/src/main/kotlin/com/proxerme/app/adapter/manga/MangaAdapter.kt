package com.proxerme.app.adapter.manga

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.manga.entity.Page
import com.proxerme.library.info.ProxerUrlHolder
import view.TouchImageView

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

        private val image: TouchImageView by bindView(R.id.image)
        private val placeholder: View by bindView(R.id.placeholder)

        override fun bind(item: Page) {
            val width = Utils.getScreenWidth(image.context)
            val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()

            placeholder.minimumHeight = height

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getMangaPageUrl(server!!, entryId!!, id!!, item.name)
                            .toString())
                    .asBitmap()
                    .override(width, height)
                    .placeholder(generatePlaceholder(width, height))
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }

        private fun generatePlaceholder(width: Int, height: Int): Drawable {
            return BitmapDrawable(itemView.context.resources,
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444).apply {
                        eraseColor(ContextCompat.getColor(itemView.context, R.color.divider))
                    })
        }
    }

}