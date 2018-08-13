package me.proxer.app.ucp.media

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.ucp.media.UcpMediaAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.entity.user.UserMediaListEntry
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class UcpMediaAdapter : BaseAdapter<UserMediaListEntry, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, UserMediaListEntry>> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_profile_media_entry, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val status: TextView by bindView(R.id.status)
        internal val state: ImageView by bindView(R.id.state)
        internal val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        internal val rating: RatingBar by bindView(R.id.rating)

        fun bind(item: UserMediaListEntry) {
            itemView.clicks()
                .mapAdapterPosition({ adapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            ViewCompat.setTransitionName(image, "ucp_media_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            status.text = item.mediaProgress.toEpisodeAppString(status.context, item.episode, item.medium.toCategory())
            state.setImageDrawable(item.state.toAppDrawable(state.context).sizeDp(16))

            if (item.rating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.rating.toFloat() / 2.0f
            } else {
                ratingContainer.visibility = View.GONE
            }

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }
    }
}
