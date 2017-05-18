package me.proxer.app.adapter.ucp

import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entitiy.ucp.UcpHistoryEntry
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class HistoryAdapter : PagingAdapter<UcpHistoryEntry>() {

    var callback: HistoryAdapterCallback? = null

    init {
        setHasStableIds(false)
    }

    override fun getItemId(position: Int) = internalList[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<UcpHistoryEntry> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_history_entry, parent, false))
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<UcpHistoryEntry>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val status: TextView by bindView(R.id.status)

        init {
            itemView.setOnClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onItemClick(view, internalList[it])
                }
            }
        }

        override fun bind(item: UcpHistoryEntry) {
            ViewCompat.setTransitionName(image, "history_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            status.text = status.context.getString(R.string.fragment_history_entry_status, item.episode,
                    TimeUtils.convertToRelativeReadableTime(status.context, item.date))

            GlideApp.with(image.context)
                    .load(ProxerUrls.entryImage(item.id).toString())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(image)
        }
    }

    interface HistoryAdapterCallback {
        fun onItemClick(view: View, item: UcpHistoryEntry) {}
    }
}