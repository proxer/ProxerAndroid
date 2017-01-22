package com.proxerme.app.adapter.info

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
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.list.entity.ProjectListEntry
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.ProjectTypeParameter
import com.proxerme.library.parameters.StateParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ProjectAdapter : PagingAdapter<ProjectListEntry>() {

    var callback: ProjectAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_project_entry, parent, false))
    }

    override fun removeCallback() {
        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<ProjectListEntry>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        private val rating: RatingBar by bindView(R.id.rating)
        private val status: TextView by bindView(R.id.status)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onItemClick(list[adapterPosition])
                }
            }
        }

        override fun bind(item: ProjectListEntry) {
            title.text = item.name
            medium.text = item.medium
            status.text = status.context.getString(generateStatusText(item.type, item.state))

            if (item.rating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.rating / 2.0f
            } else {
                ratingContainer.visibility = View.GONE
            }

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(item.id).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }

        private fun generateStatusText(type: String, state: Int): Int {
            return when (type.toIntOrNull()) {
                ProjectTypeParameter.UNDEFINED -> R.string.project_type_unkown
                ProjectTypeParameter.FINISHED -> R.string.project_type_finished
                ProjectTypeParameter.IN_WORK -> R.string.project_type_in_work
                ProjectTypeParameter.PLANNED -> R.string.project_type_planned
                ProjectTypeParameter.CANCELLED -> R.string.project_type_cancelled
                ProjectTypeParameter.LICENCED -> R.string.project_type_licenced
                null -> when (state) {
                    StateParameter.PRE_AIRING -> R.string.media_state_pre_airing
                    StateParameter.AIRING -> R.string.media_state_airing
                    StateParameter.CANCELLED -> R.string.media_state_cancelled
                    StateParameter.CANCELLED_SUB -> R.string.media_state_cancelled_sub
                    StateParameter.FINISHED -> R.string.media_state_finished
                    else -> throw IllegalArgumentException("Unknown state: $state")
                }
                else -> throw IllegalArgumentException("Unknown project type: ${type}")
            }
        }
    }

    abstract class ProjectAdapterCallback {
        open fun onItemClick(item: ProjectListEntry) {

        }
    }
}