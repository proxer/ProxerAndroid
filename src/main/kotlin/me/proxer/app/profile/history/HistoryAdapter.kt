package me.proxer.app.profile.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.profile.history.HistoryAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entity.user.UserHistoryEntry
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class HistoryAdapter : BaseAdapter<UserHistoryEntry, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, UserHistoryEntry>> = PublishSubject.create()
    val longClickSubject: PublishSubject<Pair<ImageView, UserHistoryEntry>> = PublishSubject.create()

    init {
        setHasStableIds(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_history_entry, parent, false))
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

        fun bind(item: UserHistoryEntry) {
            container.clicks()
                .mapAdapterPosition({ adapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            container.longClicks()
                .mapAdapterPosition({ adapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(longClickSubject)

            ViewCompat.setTransitionName(image, "history_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            status.text = status.context.getString(
                when (item.category) {
                    Category.ANIME -> R.string.fragment_history_entry_status_anime
                    Category.MANGA, Category.NOVEL -> R.string.fragment_history_entry_status_manga
                },
                item.episode
            )

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.entryId))
        }
    }
}
