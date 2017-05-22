package me.proxer.app.adapter.profile

import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseGlideAdapter
import me.proxer.app.application.GlideRequests
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.entitiy.user.UserMediaListEntry
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class UserMediaAdapter(glide: GlideRequests) : BaseGlideAdapter<UserMediaListEntry>(glide) {

    var callback: UserMediaAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = internalList[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<UserMediaListEntry> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_user_media_entry, parent, false))
    }

    override fun onViewRecycled(holder: BaseViewHolder<UserMediaListEntry>) {
        if (holder is ViewHolder) {
            clearImage(holder.image)
        }
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<UserMediaListEntry>(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val status: TextView by bindView(R.id.status)
        internal val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        internal val rating: RatingBar by bindView(R.id.rating)

        init {
            itemView.setOnClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onMediaClick(view, internalList[it])
                }
            }
        }

        override fun bind(item: UserMediaListEntry) {
            ViewCompat.setTransitionName(image, "user_media_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            status.text = item.mediaProgress.toEpisodeAppString(status.context, item.episode, item.medium.toCategory())

            if (item.rating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.rating.toFloat() / 2.0f
            } else {
                ratingContainer.visibility = View.GONE
            }

            loadImage(image, ProxerUrls.entryImage(item.id))
        }
    }

    interface UserMediaAdapterCallback {
        fun onMediaClick(view: View, item: UserMediaListEntry) {}
    }
}
