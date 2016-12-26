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
import com.proxerme.library.connection.info.entity.Relation
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class RelationsAdapter : PagingAdapter<Relation>() {

    var callback: RelationsAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<Relation> {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_media_entry, parent, false))
    }

    override fun removeCallback() {
        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<Relation>(itemView) {

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

        override fun bind(item: Relation) {
            title.text = item.name
            medium.text = item.medium
            episodes.text = generateEpisodeCountDescription(episodes.context, item.category,
                    item.episodeCount)

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

        private fun generateEpisodeCountDescription(context: Context,
                                                    @CategoryParameter.Category category: String,
                                                    count: Int): String {
            return when (category) {
                CategoryParameter.ANIME -> context.resources
                        .getQuantityString(R.plurals.media_episode_count, count, count)
                CategoryParameter.MANGA -> context.resources
                        .getQuantityString(R.plurals.media_chapter_count, count, count)
                else -> throw RuntimeException("Category has an illegal value")
            }
        }
    }

    abstract class RelationsAdapterCallback {
        open fun onItemClick(item: Relation) {

        }
    }

}