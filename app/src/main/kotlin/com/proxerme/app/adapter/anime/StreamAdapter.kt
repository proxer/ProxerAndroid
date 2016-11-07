package com.proxerme.app.adapter.anime

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
import com.proxerme.library.connection.anime.entity.Stream
import com.proxerme.library.info.ProxerUrlHolder

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class StreamAdapter(savedInstanceState: Bundle?) :
        PagingAdapter<Stream, StreamAdapter.StreamAdapterCallback>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_anime_state_items"
    }

    init {
        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList(ITEMS_STATE))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            PagingViewHolder<Stream, StreamAdapterCallback> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_stream,
                parent, false))
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ITEMS_STATE, list)
    }

    inner class ViewHolder(itemView: View) :
            PagingAdapter.PagingViewHolder<Stream, StreamAdapterCallback>(itemView) {

        override val adapterList: List<Stream>
            get() = list
        override val adapterCallback: StreamAdapterCallback?
            get() = callback

        private val name: TextView by bindView(R.id.name)
        private val image: ImageView by bindView(R.id.image)

        override fun bind(item: Stream) {
            name.text = item.hosterName

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getHosterImageUrl(item.imageId).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    class StreamAdapterCallback : PagingAdapter.PagingAdapterCallback<Stream>()

}