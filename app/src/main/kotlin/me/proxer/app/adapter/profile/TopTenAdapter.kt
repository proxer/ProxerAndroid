package me.proxer.app.adapter.profile

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
import me.proxer.app.util.extension.bindView
import me.proxer.library.entitiy.user.TopTenEntry
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class TopTenAdapter : PagingAdapter<TopTenEntry>() {

    var callback: TopTenAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_top_ten_entry, parent, false))
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<TopTenEntry>(itemView) {

        private val image: ImageView by bindView(R.id.image)
        private val title: TextView by bindView(R.id.title)

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

            GlideApp.with(image.context)
                    .load(ProxerUrls.entryImage(item.id).toString())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(image)
        }
    }

    interface TopTenAdapterCallback {
        fun onTopTenEntryClick(view: View, item: TopTenEntry) {}
    }
}
