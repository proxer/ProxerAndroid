package me.proxer.app.notification

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.notification.NotificationAdapter.ViewHolder
import me.proxer.app.util.compat.HtmlCompat
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.convertToRelativeReadableTime

/**
 * @author Ruben Gees
 */
class NotificationAdapter : BaseAdapter<ProxerNotification, ViewHolder>() {

    val clickSubject: PublishSubject<ProxerNotification> = PublishSubject.create()
    val deleteClickSubject: PublishSubject<ProxerNotification> = PublishSubject.create()

    init {
        setHasStableIds(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val text: TextView by bindView(R.id.text)
        internal val date: TextView by bindView(R.id.date)
        internal val delete: ImageButton by bindView(R.id.delete)

        init {
            itemView.setOnClickListener { _ ->
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(data[it])
                }
            }

            delete.setOnClickListener {
                withSafeAdapterPosition(this) {
                    deleteClickSubject.onNext(data[it])
                }
            }

            delete.setImageDrawable(IconicsDrawable(delete.context)
                    .icon(CommunityMaterial.Icon.cmd_delete)
                    .colorRes(R.color.icon)
                    .sizeDp(32)
                    .paddingDp(4))
        }

        fun bind(item: ProxerNotification) {
            text.text = HtmlCompat.fromHtml(item.text)
            date.text = item.date.convertToRelativeReadableTime(date.context)
        }
    }
}
