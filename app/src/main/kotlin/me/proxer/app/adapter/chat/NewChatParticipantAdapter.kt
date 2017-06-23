package me.proxer.app.adapter.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseGlideAdapter
import me.proxer.app.application.GlideRequests
import me.proxer.app.entity.chat.Participant
import me.proxer.app.util.extension.bindView
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class NewChatParticipantAdapter(savedInstanceState: Bundle?, glide: GlideRequests) :
        BaseGlideAdapter<Participant>(glide) {

    private companion object {
        private const val LIST_STATE = "new_chat_participants_list"
    }

    var callback: NewChatParticipantAdapterCallback? = null

    init {
        savedInstanceState?.let {
            insert(it.getParcelableArrayList(LIST_STATE))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Participant> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_participant, parent, false))
    }

    override fun onViewRecycled(holder: BaseViewHolder<Participant>) {
        if (holder is ViewHolder) {
            clearImage(holder.image)
        }
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    fun contains(username: String) = internalList.find { it.username.equals(username, ignoreCase = true) } != null

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<Participant>(itemView) {

        internal val image: ImageView by bindView(R.id.image)
        internal val username: TextView by bindView(R.id.username)
        internal val remove: ImageButton by bindView(R.id.remove)

        init {
            remove.setImageDrawable(IconicsDrawable(remove.context)
                    .icon(CommunityMaterial.Icon.cmd_close)
                    .sizeDp(48)
                    .paddingDp(16)
                    .colorRes(R.color.icon))

            remove.setOnClickListener {
                withSafeAdapterPosition {
                    internalList.removeAt(it)

                    notifyItemRemoved(it)

                    callback?.onParticipantRemoved()
                }
            }
        }

        override fun bind(item: Participant) {
            username.text = item.username

            if (item.image.isBlank()) {
                image.setImageDrawable(IconicsDrawable(image.context)
                        .icon(CommunityMaterial.Icon.cmd_account)
                        .sizeDp(96)
                        .paddingDp(16)
                        .colorRes(R.color.colorAccent))
            } else {
                loadImage(image, ProxerUrls.userImage(item.image), {
                    it.circleCrop()
                })
            }
        }
    }

    interface NewChatParticipantAdapterCallback {
        fun onParticipantRemoved() {}
    }
}