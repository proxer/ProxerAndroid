package com.proxerme.app.adapter.chat

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.messenger.entity.ConferenceInfoUser
import com.proxerme.library.info.ProxerUrlHolder
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ConferenceParticipantAdapter : PagingAdapter<ConferenceInfoUser>() {

    var leader: String? = null
    var callback: ConferenceParticipantAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<ConferenceInfoUser> {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_conference_participant, parent, false))
    }

    override fun removeCallback() {
        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<ConferenceInfoUser>(itemView) {

        private val image: ImageView by bindView(R.id.image)
        private val username: TextView by bindView(R.id.username)
        private val status: TextView by bindView(R.id.status)

        init {
            status.movementMethod = TouchableMovementMethod.getInstance()

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onItemClick(list[adapterPosition])
                }
            }
        }

        override fun bind(item: ConferenceInfoUser) {
            username.text = item.username

            if (item.id == leader) {
                username.setCompoundDrawables(null, null, IconicsDrawable(username.context)
                        .icon(CommunityMaterial.Icon.cmd_star)
                        .sizeDp(32)
                        .paddingDp(8)
                        .colorRes(R.color.colorAccent), null)
            } else {

                username.setCompoundDrawables(null, null, null, null)
            }

            if (item.status.isBlank()) {
                status.visibility = View.GONE
            } else {
                status.visibility = View.VISIBLE
                status.text = Utils.buildClickableText(status.context, item.status,
                        onWebClickListener = Link.OnClickListener { link ->
                            callback?.onStatusLinkClick(Utils.parseAndFixUrl(link))
                        })
            }

            if (item.imageId.isBlank()) {
                Glide.clear(image)

                image.setImageDrawable(IconicsDrawable(image.context)
                        .icon(CommunityMaterial.Icon.cmd_account)
                        .sizeDp(96)
                        .paddingDp(16)
                        .colorRes(R.color.colorAccent))
            } else {
                Glide.with(image.context)
                        .load(ProxerUrlHolder.getUserImageUrl(item.imageId).toString())
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(image)
            }
        }
    }

    abstract class ConferenceParticipantAdapterCallback {
        open fun onItemClick(item: ConferenceInfoUser) {

        }

        open fun onStatusLinkClick(link: HttpUrl) {

        }
    }
}