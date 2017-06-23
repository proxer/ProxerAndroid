package me.proxer.app.adapter.chat

import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseGlideAdapter
import me.proxer.app.application.GlideRequests
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.bindView
import me.proxer.library.entitiy.messenger.ConferenceParticipant
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class ConferenceParticipantAdapter(glide: GlideRequests) : BaseGlideAdapter<ConferenceParticipant>(glide) {

    var leader: String? = null
    var callback: ConferenceParticipantAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ConferenceParticipant> {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_conference_participant, parent, false))
    }

    override fun onViewRecycled(holder: BaseViewHolder<ConferenceParticipant>) {
        if (holder is ViewHolder) {
            clearImage(holder.image)
        }
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<ConferenceParticipant>(itemView) {

        internal val image: ImageView by bindView(R.id.image)
        internal val username: TextView by bindView(R.id.username)
        internal val status: TextView by bindView(R.id.status)

        init {
            status.movementMethod = TouchableMovementMethod.getInstance()

            itemView.setOnClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onParticipantClick(view, internalList[it])
                }
            }
        }

        override fun bind(item: ConferenceParticipant) {
            ViewCompat.setTransitionName(image, "conference_participant_${item.id}")

            username.text = item.username

            if (item.id == leader) {
                username.setCompoundDrawablesWithIntrinsicBounds(null, null, IconicsDrawable(username.context)
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

    interface ConferenceParticipantAdapterCallback {
        fun onParticipantClick(view: View, item: ConferenceParticipant) {}
        fun onStatusLinkClick(link: HttpUrl) {}
    }
}