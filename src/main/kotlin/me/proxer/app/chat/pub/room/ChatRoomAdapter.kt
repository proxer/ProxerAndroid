package me.proxer.app.chat.pub.room

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.TouchableMovementMethod
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.chat.pub.room.ChatRoomAdapter.ViewHolder
import me.proxer.app.util.Utils
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

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        internal val nameView by bindView<TextView>(R.id.name)
        internal val topic by bindView<TextView>(R.id.topic)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(data[it])
                }
            }

            topic.movementMethod = TouchableMovementMethod.instance
        }

        fun bind(item: ChatRoom) {
            nameView.text = item.name

            if (item.topic.isBlank()) {
                topic.visibility = View.GONE
                topic.text = item.topic
            } else {
                topic.visibility = View.VISIBLE
                topic.text = Utils.buildClickableText(topic.context, item.topic.trim(), onWebClickListener = {
                    Utils.safelyParseAndFixUrl(it)?.let { url -> linkClickSubject.onNext(url) }
                })
            }
        }
    }
}
