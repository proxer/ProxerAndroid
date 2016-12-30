package com.proxerme.app.adapter.ucp

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
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.ucp.entitiy.UcpToptenEntry
import com.proxerme.library.info.ProxerUrlHolder

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UcpToptenAdapter : PagingAdapter<UcpToptenEntry>() {

    var callback: UcpToptenAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            PagingViewHolder<UcpToptenEntry> {
        return ReminderViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ucp_topten_entry, parent, false))
    }

    override fun removeCallback() {
        callback = null
    }

    inner class ReminderViewHolder(itemView: View) : PagingViewHolder<UcpToptenEntry>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val image: ImageView by bindView(R.id.image)
        private val removeButton: ImageButton by bindView(R.id.removeButton)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onItemClick(list[adapterPosition])
                }
            }

            removeButton.setImageDrawable(IconicsDrawable(removeButton.context)
                    .icon(CommunityMaterial.Icon.cmd_star_off)
                    .colorRes(R.color.icon)
                    .sizeDp(48)
                    .paddingDp(12))

            removeButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onRemoveClick(list[adapterPosition])
                }
            }
        }

        override fun bind(item: UcpToptenEntry) {
            title.text = item.name

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(item.entryId).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    abstract class UcpToptenAdapterCallback {

        open fun onItemClick(item: UcpToptenEntry) {

        }

        open fun onRemoveClick(item: UcpToptenEntry) {

        }
    }
}