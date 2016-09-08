package com.proxerme.app.adapter.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.adapter.user.ToptenAdapter.ToptenAdapterCallback
import com.proxerme.library.connection.user.entitiy.ToptenEntry
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ToptenAdapter(savedInstanceState: Bundle? = null,
                    @CategoryParameter.Category private val category: String) :
        PagingAdapter<ToptenEntry, ToptenAdapterCallback>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_topten_state_items"
    }

    init {
        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList("${ITEMS_STATE}_$category"))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_topten_entry, parent, false))

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList("${ITEMS_STATE}_$category", list)
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<ToptenEntry,
            ToptenAdapterCallback>(itemView) {

        override val adapterList: List<ToptenEntry>
            get() = list
        override val adapterCallback: ToptenAdapterCallback?
            get() = callback

        private val image: ImageView by bindView(R.id.image)
        private val title: TextView by bindView(R.id.title)

        override fun bind(item: ToptenEntry) {
            title.text = item.name

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(item.id).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    class ToptenAdapterCallback : PagingAdapterCallback<ToptenEntry>()
}