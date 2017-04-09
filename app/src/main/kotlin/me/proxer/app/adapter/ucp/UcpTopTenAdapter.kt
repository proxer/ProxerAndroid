package me.proxer.app.adapter.ucp

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.util.extension.bindView
import me.proxer.library.entitiy.ucp.UcpTopTenEntry
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class UcpTopTenAdapter : PagingAdapter<UcpTopTenEntry>() {

    var callback: UcpToptenAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = internalList[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<UcpTopTenEntry> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_ucp_top_ten_entry, parent, false))
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<UcpTopTenEntry>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val image: ImageView by bindView(R.id.image)
        private val removeButton: ImageButton by bindView(R.id.removeButton)

        init {
            itemView.setOnClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onItemClick(view, internalList[it])
                }
            }

            removeButton.setImageDrawable(IconicsDrawable(removeButton.context)
                    .icon(CommunityMaterial.Icon.cmd_star_off)
                    .colorRes(R.color.icon)
                    .sizeDp(48)
                    .paddingDp(12))

            removeButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onRemoveClick(internalList[adapterPosition])
                }
            }
        }

        override fun bind(item: UcpTopTenEntry) {
            ViewCompat.setTransitionName(image, "ucp_top_ten_${item.id}")

            title.text = item.name

            Glide.with(image.context)
                    .load(ProxerUrls.entryImage(item.entryId).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    interface UcpToptenAdapterCallback {
        fun onItemClick(view: View, item: UcpTopTenEntry) {}
        fun onRemoveClick(item: UcpTopTenEntry) {}
    }
}