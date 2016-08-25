package com.proxerme.app.adapter

import android.content.Context
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
import com.proxerme.library.connection.list.entity.MediaListEntry
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import com.proxerme.library.parameters.CategoryParameter.ANIME
import com.proxerme.library.parameters.CategoryParameter.MANGA

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MediaAdapter(savedInstanceState: Bundle? = null,
                   @CategoryParameter.Category private val category: String) :
        PagingAdapter<MediaListEntry>(savedInstanceState) {

    private companion object {
        private const val STATE_ITEMS = "adapter_media_state_items"
    }

    override val stateKey = "${STATE_ITEMS}_$category"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_media_entry, parent, false))
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<MediaListEntry>(itemView) {

        init {
            itemView.setOnClickListener {
                //TODO
            }
        }

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val rating: RatingBar by bindView(R.id.rating)
        private val ratingAmount: TextView by bindView(R.id.ratingAmount)
        private val genres: TextView by bindView(R.id.genres)
        private val episodes: TextView by bindView(R.id.episodes)
        private val languages: TextView by bindView(R.id.languages)

        override fun bind(item: MediaListEntry) {
            title.text = item.name
            medium.text = item.medium
            genres.text = item.genres.joinToString(", ")
            episodes.text = generateEpisodeCountDescription(episodes.context, item.episodeCount)
            languages.text = item.languages.joinToString(", ")

            if (item.rating > 0) {
                rating.visibility = View.VISIBLE
                rating.rating = item.rating.toFloat() / 2.0f
                ratingAmount.visibility = View.VISIBLE
                ratingAmount.text = "(${item.rateCount.toString()})"
            } else {
                rating.visibility = View.GONE
                ratingAmount.visibility = View.GONE
            }

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(item.id).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }

        private fun generateEpisodeCountDescription(context: Context, count: Int): String {
            return when (category) {
                ANIME -> context.resources
                        .getQuantityString(R.plurals.media_episode_count, count, count)
                MANGA -> context.resources
                        .getQuantityString(R.plurals.media_chapter_count, count, count)
                else -> throw RuntimeException("Category has an illegal value")
            }
        }
    }
}