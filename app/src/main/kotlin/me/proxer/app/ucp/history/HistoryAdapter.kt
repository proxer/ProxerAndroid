package me.proxer.app.ucp.history

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.ucp.history.HistoryAdapter.ViewHolder
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entity.ucp.UcpHistoryEntry
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class HistoryAdapter : BaseAdapter<UcpHistoryEntry, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, UcpHistoryEntry>> = PublishSubject.create()

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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val status: TextView by bindView(R.id.status)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(image to data[it])
                }
            }
        }

        fun bind(item: UcpHistoryEntry) {
            ViewCompat.setTransitionName(image, "history_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            status.text = status.context.getString(R.string.fragment_history_entry_status, item.episode,
                    item.date.convertToRelativeReadableTime(status.context))

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }
    }
}
