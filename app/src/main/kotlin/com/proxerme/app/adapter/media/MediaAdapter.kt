package com.proxerme.app.adapter.media

import android.content.Context
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
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
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
class MediaAdapter(@CategoryParameter.Category private val category: String) :
        PagingAdapter<MediaListEntry>() {

    var callback: MediaAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_media_entry, parent, false))
    }

    override fun removeCallback() {
        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<MediaListEntry>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val rating: RatingBar by bindView(R.id.rating)
        private val episodes: TextView by bindView(R.id.episodes)
        private val english: ImageView by bindView(R.id.english)
        private val german: ImageView by bindView(R.id.german)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onItemClick(list[adapterPosition])
                }
            }
        }

        override fun bind(item: MediaListEntry) {
            title.text = item.name
            medium.text = item.medium
            episodes.text = generateEpisodeCountDescription(episodes.context, item.episodeCount)

            val languages = Utils.getLanguages(*item.languages)

            english.visibility = when (languages.contains(Utils.Language.ENGLISH)) {
                true -> View.VISIBLE
                false -> View.GONE
            }

            german.visibility = when (languages.contains(Utils.Language.GERMAN)) {
                true -> View.VISIBLE
                false -> View.GONE
            }

            if (item.rating > 0) {
                rating.visibility = View.VISIBLE
                rating.rating = item.rating / 2.0f
            } else {
                rating.visibility = View.GONE
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

    abstract class MediaAdapterCallback {
        open fun onItemClick(item: MediaListEntry) {

        }
    }
}