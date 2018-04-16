package me.proxer.app.chat.pub.room.info

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.klinker.android.link_builder.TouchableMovementMethod
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.chat.pub.room.info.ChatRoomUserAdapter.ViewHolder
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.colorRes
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.library.entity.chat.ChatRoomUser
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class ChatRoomUserAdapter : BaseAdapter<ChatRoomUser, ViewHolder>() {

    var glide: GlideRequests? = null
    val participantClickSubject: PublishSubject<Pair<ImageView, ChatRoomUser>> = PublishSubject.create()
    val statusLinkClickSubject: PublishSubject<HttpUrl> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_room_participant, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val image: ImageView by bindView(R.id.image)
        internal val username: TextView by bindView(R.id.username)
        internal val status: TextView by bindView(R.id.status)

        init {
            status.movementMethod = TouchableMovementMethod.instance

            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    participantClickSubject.onNext(image to data[it])
                }
            }
        }

        fun bind(item: ChatRoomUser) {
            ViewCompat.setTransitionName(image, "chat_room_user_${item.id}")

            username.text = item.name

            if (item.isModerator) {
                username.setCompoundDrawablesWithIntrinsicBounds(null, null, IconicsDrawable(username.context)
                    .icon(CommunityMaterial.Icon.cmd_star)
                    .sizeDp(32)
                    .paddingDp(8)
                    .colorRes(username.context, R.color.colorAccent), null)
            } else {
                username.setCompoundDrawables(null, null, null, null)
            }

            if (item.status.isBlank()) {
                status.visibility = View.GONE
            } else {
                status.visibility = View.VISIBLE
                status.text = Utils.buildClickableText(status.context, item.status, onWebClickListener = { link ->
                    statusLinkClickSubject.onNext(Utils.parseAndFixUrl(link))
                })
            }

            if (item.image.isBlank()) {
                image.setIconicsImage(CommunityMaterial.Icon.cmd_account, 96, 16, R.color.colorAccent)
            } else {
                glide?.load(ProxerUrls.userImage(item.image).toString())
                    ?.transition(DrawableTransitionOptions.withCrossFade())
                    ?.circleCrop()
                    ?.into(image)
            }
        }
    }
}
