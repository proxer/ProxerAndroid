package me.proxer.app.adapter.profile

import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.application.GlideRequests
import me.proxer.app.util.extension.bindView
import me.proxer.library.entitiy.user.TopTenEntry
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class TopTenAdapter(private val glide: GlideRequests) : BaseAdapter<TopTenEntry>() {

    var callback: TopTenAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<TopTenEntry> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_top_ten_entry, parent, false))
    }

    override fun onViewRecycled(holder: BaseViewHolder<TopTenEntry>) {
        if (holder is ViewHolder) {
            glide.clear(holder.image)
        }
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<TopTenEntry>(itemView) {

        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)

        init {
            itemView.setOnClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onTopTenEntryClick(view, internalList[it])
                }
            }
        }

        override fun bind(item: TopTenEntry) {
            ViewCompat.setTransitionName(image, "top_ten_${item.id}")

            title.text = item.name

            glide.load(ProxerUrls.entryImage(item.id).toString())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(image)
        }
    }

    interface TopTenAdapterCallback {
        fun onTopTenEntryClick(view: View, item: TopTenEntry) {}
    }
}
