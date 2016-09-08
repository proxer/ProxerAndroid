package com.proxerme.app.adapter.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.adapter.user.UserMediaAdapter.UserMediaAdapterCallback
import com.proxerme.library.connection.user.entitiy.UserMediaListEntry
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import com.proxerme.library.parameters.CommentStateParameter.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UserMediaAdapter(savedInstanceState: Bundle? = null,
                       @CategoryParameter.Category private val category: String) :
        PagingAdapter<UserMediaListEntry, UserMediaAdapterCallback>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_user_media_state_items"
    }

    init {
        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList("${ITEMS_STATE}_$category"))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_media_entry, parent, false))
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList("${ITEMS_STATE}_$category", list)
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<UserMediaListEntry,
            UserMediaAdapterCallback>(itemView) {

        override val adapterList: List<UserMediaListEntry>
            get() = list
        override val adapterCallback: UserMediaAdapterCallback?
            get() = callback

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val status: TextView by bindView(R.id.status)
        private val rating: RatingBar by bindView(R.id.rating)

        override fun bind(item: UserMediaListEntry) {
            title.text = item.name
            medium.text = item.medium
            status.text = "${item.commentEpisode}/${item.episodeCount} - " +
                    "${convertStateToText(item)}"

            if (item.commentRating > 0) {
                rating.visibility = View.VISIBLE
                rating.rating = item.commentRating.toFloat() / 2.0f
            } else {
                rating.visibility = View.GONE
            }

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(item.id).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }

        fun convertStateToText(entry: UserMediaListEntry): String {
            if (category == CategoryParameter.ANIME) {
                return when (entry.commentState) {
                    WATCHED -> itemView.context.getString(R.string.user_media_state_watched)
                    WATCHING -> itemView.context.getString(R.string.user_media_state_watching)
                    WILL_WATCH -> itemView.context.getString(R.string.user_media_state_will_watch)
                    CANCELLED -> itemView.context.getString(R.string.user_media_state_cancelled)
                    else -> throw IllegalArgumentException("Illegal comment state")
                }
            } else {
                return when (entry.commentState) {
                    WATCHED -> itemView.context.getString(R.string.user_media_state_read)
                    WATCHING -> itemView.context.getString(R.string.user_media_state_reading)
                    WILL_WATCH -> itemView.context.getString(R.string.user_media_state_will_read)
                    CANCELLED -> itemView.context.getString(R.string.user_media_state_cancelled)
                    else -> throw IllegalArgumentException("Illegal comment state")
                }
            }
        }
    }

    abstract class UserMediaAdapterCallback : PagingAdapterCallback<UserMediaListEntry>()
}