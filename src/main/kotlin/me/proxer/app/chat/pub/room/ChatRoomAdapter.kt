package me.proxer.app.chat.pub.room

import android.support.v7.widget.AppCompatTextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.chat.pub.room.ChatRoomAdapter.ViewHolder
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.fastText
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.setSimpleOnLinkClickListener
import me.proxer.library.entity.chat.ChatRoom
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class ChatRoomAdapter : BaseAdapter<ChatRoom, ViewHolder>() {

    val clickSubject: PublishSubject<ChatRoom> = PublishSubject.create()
    val linkClickSubject: PublishSubject<HttpUrl> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_room, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    inner class ViewHolder(view: View) : AutoDisposeViewHolder(view) {

        internal val nameView by bindView<TextView>(R.id.name)
        internal val topic by bindView<AppCompatTextView>(R.id.topic)

        init {
            topic.setSimpleOnLinkClickListener { _, link ->
                Utils.parseAndFixUrl(link)?.let { url -> linkClickSubject.onNext(url) }
            }
        }

        fun bind(item: ChatRoom) {
            itemView.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            nameView.text = item.name

            if (item.topic.isBlank()) {
                topic.visibility = View.GONE
                topic.text = item.topic
            } else {
                topic.visibility = View.VISIBLE
                topic.fastText = item.topic.trim().linkify(mentions = false)
            }
        }
    }
}
