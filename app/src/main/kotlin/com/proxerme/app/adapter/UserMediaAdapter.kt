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
import com.proxerme.library.connection.user.entitiy.UserMediaListEntry
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import com.proxerme.library.parameters.UserMediaSortParameter
import java.util.*
import kotlin.comparisons.compareBy
import kotlin.comparisons.compareByDescending
import kotlin.comparisons.thenBy
import kotlin.comparisons.thenByDescending

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UserMediaAdapter(savedInstanceState: Bundle?,
                       @CategoryParameter.Category private val category: String,
                       @UserMediaSortParameter.UserMediaSortCriteria var sortCriteria: String) :
        RecyclerView.Adapter<UserMediaAdapter.ViewHolder>() {

    private companion object {
        const val STATE_ITEMS = "adapter_user_media_state_items"
    }

    private val list = ArrayList<UserMediaListEntry>()

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
                .inflate(R.layout.item_user_media_entry, parent, false))
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    fun clear() {
        list.clear()

        notifyDataSetChanged()
    }

    fun addItems(newItems: Collection<UserMediaListEntry>) {
        val comparator = generateComparator()

        list.addAll(newItems.filter {
            list.binarySearch(it, comparator) < 0
        })
        list.sortWith(comparator)

        notifyDataSetChanged()
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_ITEMS, list)
    }

    private fun generateComparator(): Comparator<in UserMediaListEntry> {
        return when (sortCriteria) {
            UserMediaSortParameter.NAME_ASCENDING -> compareBy { it.name }
            UserMediaSortParameter.NAME_DESCENDING -> compareByDescending { it.name }
            UserMediaSortParameter.STATE_NAME_ASCENDING -> {
                compareBy<UserMediaListEntry> { it.commentState }.thenBy { it.name }
            }
            UserMediaSortParameter.STATE_NAME_DESCENDING -> {
                compareBy<UserMediaListEntry> { it.commentState }
                        .thenByDescending { it.name }
            }
            else -> compareBy { it.name }
        }
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
        private val status: TextView by bindView(R.id.status)
        private val rating: RatingBar by bindView(R.id.rating)

        fun bind(entry: UserMediaListEntry) {
            title.text = entry.name
            medium.text = entry.medium
            status.text = "${entry.commentEpisode}/${entry.episodeCount} - " +
                    "${convertStateToText(entry)}"

            if (entry.commentRating > 0) {
                rating.visibility = View.VISIBLE
                rating.rating = entry.commentRating.toFloat() / 2.0f
            } else {
                rating.visibility = View.GONE
            }

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(entry.id))
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }

        fun convertStateToText(entry: UserMediaListEntry): String {
            if (category == CategoryParameter.ANIME) {
                return when (entry.commentState) {
                    0 -> itemView.context.getString(R.string.user_media_state_watched)
                    1 -> itemView.context.getString(R.string.user_media_state_watching)
                    2 -> itemView.context.getString(R.string.user_media_state_will_watch)
                    3 -> itemView.context.getString(R.string.user_media_state_cancelled)
                    else -> throw IllegalArgumentException("The state must be between 0 and 3")
                }
            } else {
                return when (entry.commentState) {
                    0 -> itemView.context.getString(R.string.user_media_state_read)
                    1 -> itemView.context.getString(R.string.user_media_state_reading)
                    2 -> itemView.context.getString(R.string.user_media_state_will_read)
                    3 -> itemView.context.getString(R.string.user_media_state_cancelled)
                    else -> throw IllegalArgumentException("The state must be between 0 and 3")
                }
            }
        }
    }
}