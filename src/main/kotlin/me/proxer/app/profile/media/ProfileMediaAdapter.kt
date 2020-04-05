package me.proxer.app.profile.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.sizeDp
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.profile.media.ProfileMediaAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class ProfileMediaAdapter : BaseAdapter<LocalUserMediaListEntry, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, LocalUserMediaListEntry>> = PublishSubject.create()
    val deleteClickSubject: PublishSubject<LocalUserMediaListEntry> = PublishSubject.create()

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

        internal val container: ViewGroup by bindView(R.id.container)
        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val status: TextView by bindView(R.id.status)
        internal val state: ImageView by bindView(R.id.state)
        internal val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        internal val rating: RatingBar by bindView(R.id.rating)
        internal val delete: ImageButton by bindView(R.id.delete)

        init {
            delete.setIconicsImage(CommunityMaterial.Icon3.cmd_close_circle, 48)
        }

        fun bind(item: LocalUserMediaListEntry) {
            container.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            ViewCompat.setTransitionName(image, "user_media_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            status.text = item.mediaProgress.toEpisodeAppString(status.context, item.episode, item.medium.toCategory())
            state.setImageDrawable(item.state.toAppDrawable(state.context).sizeDp(16))

            if (item.rating > 0) {
                ratingContainer.isVisible = true
                rating.rating = item.rating.toFloat() / 2.0f
            } else {
                ratingContainer.isGone = true
            }

            if (item is LocalUserMediaListEntry.Ucp) {
                delete.isVisible = true

                delete.clicks()
                    .mapBindingAdapterPosition({ bindingAdapterPosition }) { data[it] }
                    .autoDisposable(this)
                    .subscribe(deleteClickSubject)
            } else {
                delete.setOnClickListener(null)
                delete.isVisible = false
            }

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }
    }
}
