package com.proxerme.app.adapter.media

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
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.adapter.media.MediaAdapter.MediaAdapterCallback
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.list.entity.MediaListEntry
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import com.proxerme.library.parameters.CategoryParameter.ANIME
import com.proxerme.library.parameters.CategoryParameter.MANGA
import org.apmem.tools.layouts.FlowLayout

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MediaAdapter(savedInstanceState: Bundle? = null,
                   @CategoryParameter.Category private val category: String) :
        PagingAdapter<MediaListEntry, MediaAdapterCallback>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_media_state_items"
    }

    init {
        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList("${ITEMS_STATE}_$category"))
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_media_entry, parent, false))
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList("${ITEMS_STATE}_$category", list)
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<MediaListEntry,
            MediaAdapterCallback>(itemView) {

        override val adapterList: List<MediaListEntry>
            get() = list
        override val adapterCallback: MediaAdapterCallback?
            get() = callback

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val rating: RatingBar by bindView(R.id.rating)
        private val ratingAmount: TextView by bindView(R.id.ratingAmount)
        private val genres: FlowLayout by bindView(R.id.genres)
        private val episodes: TextView by bindView(R.id.episodes)
        private val languages: TextView by bindView(R.id.languages)

        override fun bind(item: MediaListEntry) {
            title.text = item.name
            medium.text = item.medium
            episodes.text = generateEpisodeCountDescription(episodes.context, item.episodeCount)
            languages.text = item.languages.joinToString(", ")

            if (genres.childCount < item.genres.size) {
                for (i in 0 until item.genres.size - genres.childCount) {
                    View.inflate(genres.context, R.layout.item_badge, genres)
                }
            } else if (genres.childCount > item.genres.size) {
                for (i in 0 until genres.childCount - item.genres.size) {
                    genres.removeViewAt(0)
                }
            }

            for (i in 0 until item.genres.size) {
                Utils.buildBadgeViewEntry(genres, item.genres[i], { it }, null,
                        textSizeSp = 10f, imageViewToReuse = genres.getChildAt(i) as ImageView)
                genres.getChildAt(i).visibility = View.VISIBLE
            }

            if (item.rating > 0) {
                rating.visibility = View.VISIBLE
                rating.rating = item.rating.toFloat() / 2.0f
                ratingAmount.visibility = View.VISIBLE
                ratingAmount.text = "(${item.rateCount})"
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

    abstract class MediaAdapterCallback : PagingAdapterCallback<MediaListEntry>()
}