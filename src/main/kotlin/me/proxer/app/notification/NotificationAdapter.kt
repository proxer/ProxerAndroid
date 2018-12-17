package me.proxer.app.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.text.parseAsHtml
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.notification.NotificationAdapter.ViewHolder
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.setIconicsImage

/**
 * @author Ruben Gees
 */
class NotificationAdapter : BaseAdapter<ProxerNotification, ViewHolder>() {

    val clickSubject: PublishSubject<ProxerNotification> = PublishSubject.create()
    val deleteClickSubject: PublishSubject<ProxerNotification> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val container: ViewGroup by bindView(R.id.container)
        internal val text: TextView by bindView(R.id.text)
        internal val date: TextView by bindView(R.id.date)
        internal val delete: ImageButton by bindView(R.id.delete)

        init {
            delete.setIconicsImage(CommunityMaterial.Icon.cmd_delete, 32, 4)
        }

        fun bind(item: ProxerNotification) {
            container.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            delete.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe(deleteClickSubject)

            text.text = item.text.parseAsHtml()
            date.text = item.date.convertToRelativeReadableTime(date.context)
        }
    }
}
