package com.proxerme.app.adapter

import android.os.Bundle
import android.support.v7.widget.RecyclerView
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
import com.proxerme.library.connection.parameters.CategoryParameter.*
import com.proxerme.library.info.ProxerUrlHolder
import java.util.*
import kotlin.comparisons.compareBy

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MediaAdapter(savedInstanceState: Bundle?,
                   @Category private val category: String) :
        RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

    private companion object {
        const val STATE_ITEMS = "adapter_media_state_items"
    }

    private val list = ArrayList<MediaListEntry>()

    init {
        setHasStableIds(true)

        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList(STATE_ITEMS))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_media_entry, parent, false))
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    fun clear() {
        list.clear()

        notifyDataSetChanged()
    }

    fun addItems(newItems: Collection<MediaListEntry>) {
        list.addAll(newItems.filter { list.binarySearch(it, compareBy { it.name }) < 0 })
        list.sortBy { it.name }

        notifyDataSetChanged()
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_ITEMS, list)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener {
                //TODO
            }
        }

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val rating: RatingBar by bindView(R.id.rating)
        private val genres: TextView by bindView(R.id.genres)
        private val episodes: TextView by bindView(R.id.episodes)
        private val languages: TextView by bindView(R.id.languages)

        fun bind(entry: MediaListEntry) {
            title.text = entry.name
            medium.text = entry.medium
            genres.text = entry.genres.joinToString(", ")
            episodes.text = generateEpisodeCountDescription(entry.episodeCount)
            languages.text = entry.languages.joinToString(", ")

            if (entry.rating > 0) {
                rating.visibility = View.VISIBLE
                rating.rating = entry.rating.toFloat() / 2.0f
            } else {
                rating.visibility = View.GONE
            }

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(entry.id))
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }

        private fun generateEpisodeCountDescription(count: Int): String {
            return when (category) {
                ANIME -> "${count} Episoden"
                MANGA -> "${count} Kapitel"
                else -> throw RuntimeException("Category has an illegal value")
            }
        }
    }
}