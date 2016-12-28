package com.proxerme.app.adapter.chat

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
import com.proxerme.app.entitiy.Participant
import com.proxerme.app.util.bindView
import com.proxerme.library.info.ProxerUrlHolder
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class NewChatParticipantAdapter : RecyclerView.Adapter<NewChatParticipantAdapter.ViewHolder>() {

    val participants = ArrayList<Participant>()
    var callback: NewChatParticipantAdapterCallback? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(participants[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_participant, parent, false))
    }

    override fun getItemCount() = participants.size

    fun add(participant: Participant) {
        participants.add(participant)

        notifyItemInserted(participants.size - 1)
    }

    fun contains(username: String): Boolean {
        return participants.find { it.username.equals(username, ignoreCase = true) } != null
    }

    fun isEmpty() = participants.isEmpty()

    fun removeCallback() {
        callback = null
    }

    inner class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView by bindView(R.id.image)
        private val username: TextView by bindView(R.id.username)
        private val remove: ImageButton by bindView(R.id.remove)

        init {
            remove.setImageDrawable(IconicsDrawable(remove.context)
                    .icon(CommunityMaterial.Icon.cmd_close)
                    .sizeDp(48)
                    .paddingDp(16)
                    .colorRes(R.color.icon))
            remove.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    participants.removeAt(adapterPosition)

                    notifyItemRemoved(adapterPosition)

                    callback?.onParticipantRemoved()
                }
            }
        }

        fun bind(participant: Participant) {
            username.text = participant.username

            if (participant.imageId.isBlank()) {
                Glide.clear(image)

                image.setImageDrawable(IconicsDrawable(image.context)
                        .icon(CommunityMaterial.Icon.cmd_account)
                        .sizeDp(96)
                        .paddingDp(16)
                        .colorRes(R.color.colorAccent))
            } else {
                Glide.with(image.context)
                        .load(ProxerUrlHolder.getUserImageUrl(participant.imageId).toString())
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(image)
            }
        }
    }

    abstract class NewChatParticipantAdapterCallback {
        open fun onParticipantRemoved() {

        }
    }
}