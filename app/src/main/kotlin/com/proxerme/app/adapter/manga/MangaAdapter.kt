package com.proxerme.app.adapter.manga

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
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
class MangaAdapter(savedInstanceState: Bundle?) : PagingAdapter<Page>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_manga_state_items"
        private const val SERVER_STATE = "adapter_manga_state_server"
        private const val ENTRY_ID_STATE = "adapter_manga_state_entry_id"
        private const val ID_STATE = "adapter_manga_state_id"
    }

    private var server: String? = null
    private var entryId: String? = null
    private var id: String? = null

    init {
        savedInstanceState?.let {
            server = it.getString(SERVER_STATE)
            entryId = it.getString(ENTRY_ID_STATE)
            id = it.getString(ID_STATE)

            list.addAll(it.getParcelableArrayList(ITEMS_STATE))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<Page> {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_manga_page, parent, false))
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putString(SERVER_STATE, server)
        outState.putString(ENTRY_ID_STATE, entryId)
        outState.putString(ID_STATE, id)
        outState.putParcelableArrayList(ITEMS_STATE, list)
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