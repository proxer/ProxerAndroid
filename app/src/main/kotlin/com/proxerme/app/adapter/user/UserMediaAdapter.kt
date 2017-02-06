package com.proxerme.app.adapter.user

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.ParameterMapper
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.user.entitiy.UserMediaListEntry
import com.proxerme.library.info.ProxerUrlHolder

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UserMediaAdapter : PagingAdapter<UserMediaListEntry>() {

    var callback: UserMediaAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_media_entry, parent, false))
    }

    override fun removeCallback() {
        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<UserMediaListEntry>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val status: TextView by bindView(R.id.status)
        private val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        private val rating: RatingBar by bindView(R.id.rating)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onItemClick(list[adapterPosition])
                }
            }
        }

        override fun bind(item: UserMediaListEntry) {
            title.text = item.name
            medium.text = ParameterMapper.medium(medium.context, item.medium)
            status.text = ParameterMapper.commentState(status.context,
                    ParameterMapper.mediumToCategory(item.medium) ?: "",
                    item.commentState, item.commentEpisode)

            if (item.commentRating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.commentRating.toFloat() / 2.0f
            } else {
                ratingContainer.visibility = View.GONE
            }

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(item.id).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    abstract class UserMediaAdapterCallback {
        open fun onItemClick(item: UserMediaListEntry) {

        }
    }
}