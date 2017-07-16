package me.proxer.app.adapter.ucp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.compat.HtmlCompat
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.bindView

/**
 * @author Ruben Gees
 */
class NotificationAdapter : BaseAdapter<ProxerNotification>() {

    var callback: NotificationAdapterCallback? = null

    init {
        setHasStableIds(false)
    }

    override fun getItemId(position: Int) = internalList[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ProxerNotification> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false))
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<ProxerNotification>(itemView) {

        internal val text: TextView by bindView(R.id.text)
        internal val date: TextView by bindView(R.id.date)
        internal val delete: ImageButton by bindView(R.id.delete)

        init {
            itemView.setOnClickListener { _ ->
                withSafeAdapterPosition {
                    callback?.onItemClick(internalList[it])
                }
            }

            delete.setImageDrawable(IconicsDrawable(delete.context)
                    .icon(CommunityMaterial.Icon.cmd_delete)
                    .colorRes(R.color.icon)
                    .sizeDp(32)
                    .paddingDp(4))

            delete.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onDeleteClick(internalList[it])
                }
            }
        }

        override fun bind(item: ProxerNotification) {
            text.text = HtmlCompat.fromHtml(item.text)
            date.text = TimeUtils.convertToRelativeReadableTime(date.context, item.date)
        }
    }

    interface NotificationAdapterCallback {
        fun onItemClick(item: ProxerNotification) = Unit
        fun onDeleteClick(item: ProxerNotification) = Unit
    }
}
