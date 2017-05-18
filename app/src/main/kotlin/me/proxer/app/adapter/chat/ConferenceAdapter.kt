package me.proxer.app.adapter.chat

import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import fisk.chipcloud.ChipCloud
import fisk.chipcloud.ChipCloudConfig
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.application.GlideRequests
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.extension.bindView
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class ConferenceAdapter(private val glide: GlideRequests) : PagingAdapter<LocalConference>() {

    var callback: ConferenceAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_conference, parent, false))
    }

    override fun onViewRecycled(holder: PagingViewHolder<LocalConference>) {
        if (holder is ViewHolder) {
            glide.clear(holder.image)
        }
    }

    override fun getItemId(position: Int): Long = list[position].localId

    override fun destroy() {
        super.destroy()

        callback = null
    }

    override fun insert(items: Iterable<LocalConference>) {
        doUpdates(items.plus(list.filterNot { oldItem ->
            items.any { areItemsTheSame(oldItem, it) }
        }).sortedByDescending { it.date })
    }

    override fun append(items: Iterable<LocalConference>) {
        doUpdates(list.filterNot { oldItem ->
            items.any { areItemsTheSame(oldItem, it) }
        }.plus(items).sortedByDescending { it.date })
    }

    override fun areItemsTheSame(oldItem: LocalConference, newItem: LocalConference): Boolean {
        return oldItem.localId == newItem.localId
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<LocalConference>(itemView) {

        internal val image: ImageView by bindView(R.id.image)
        internal val topic: TextView by bindView(R.id.topic)
        internal val newMessages: ViewGroup by bindView(R.id.newMessages)
        internal val time: TextView by bindView(R.id.time)
        internal val participants: TextView by bindView(R.id.participants)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onConferenceClick(internalList[it])
                }
            }
        }

        override fun bind(item: LocalConference) {
            topic.text = item.topic
            time.text = TimeUtils.convertToRelativeReadableTime(time.context, item.date)
            participants.text = participants.context.resources.getQuantityString(
                    R.plurals.fragment_conference_participants, item.participantAmount, item.participantAmount)

            if (item.localIsRead) {
                newMessages.visibility = View.GONE

                newMessages.removeAllViews()
            } else {
                newMessages.visibility = View.VISIBLE

                ChipCloud(newMessages.context, newMessages, ChipCloudConfig()
                        .uncheckedChipColor(ContextCompat.getColor(newMessages.context, R.color.colorAccent))
                        .uncheckedTextColor(ContextCompat.getColor(newMessages.context, android.R.color.white))
                        .selectMode(ChipCloud.SelectMode.none)
                        .typeface(Typeface.DEFAULT_BOLD))
                        .apply {
                            addChip(item.unreadMessageAmount)
                        }
            }

            if (item.image.isBlank()) {
                val icon = IconicsDrawable(image.context)
                        .sizeDp(96)
                        .paddingDp(16)
                        .colorRes(R.color.colorAccent)

                if (item.isGroup) {
                    icon.icon(CommunityMaterial.Icon.cmd_account_multiple)
                } else {
                    icon.icon(CommunityMaterial.Icon.cmd_account)
                }

                image.setImageDrawable(icon)
            } else {
                glide.load(ProxerUrls.userImage(item.image).toString())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(image)
            }
        }
    }

    interface ConferenceAdapterCallback {
        fun onConferenceClick(item: LocalConference) {}
    }
}
