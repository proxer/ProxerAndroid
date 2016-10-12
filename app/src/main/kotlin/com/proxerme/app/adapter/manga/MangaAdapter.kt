package com.proxerme.app.adapter.manga

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.manga.entity.Page
import com.proxerme.library.info.ProxerUrlHolder

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MangaAdapter(savedInstanceState: Bundle?) :
        PagingAdapter<Page, MangaAdapter.MangaAdapterCallback>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            PagingViewHolder<Page, MangaAdapterCallback> {
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

    inner class ViewHolder(itemView: View) : PagingViewHolder<Page, MangaAdapterCallback>(itemView) {

        override val adapterList: List<Page>
            get() = list
        override val adapterCallback: MangaAdapterCallback?
            get() = callback

        override fun bind(item: Page) {
            itemView as ImageView

            val screenWidth = Utils.getScreenWidth(itemView.context)
            val scaleFactor = screenWidth.toFloat() / item.width.toFloat()

            itemView.layoutParams.width = screenWidth
            itemView.layoutParams.height = (item.height * scaleFactor).toInt()
            itemView.requestLayout()

            Glide.with(itemView.context)
                    .load(ProxerUrlHolder.getMangaPageUrl(server!!, entryId!!, id!!, item.name)
                            .toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(itemView)
        }
    }

    class MangaAdapterCallback : PagingAdapter.PagingAdapterCallback<Page>()

}