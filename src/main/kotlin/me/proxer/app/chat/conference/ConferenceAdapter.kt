package me.proxer.app.chat.conference

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.matrixxun.starry.badgetextview.MaterialBadgeTextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.conference.ConferenceAdapter.ViewHolder
import me.proxer.app.util.extension.colorRes
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.getQuantityString
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class ConferenceAdapter : BaseAdapter<LocalConference, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<LocalConference> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = data[position].id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_conference, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    override fun areItemsTheSame(old: LocalConference, new: LocalConference) = old.id == new.id

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val image: ImageView by bindView(R.id.image)
        internal val topic: TextView by bindView(R.id.topic)
        internal val newMessages: MaterialBadgeTextView by bindView(R.id.newMessages)
        internal val time: TextView by bindView(R.id.time)
        internal val participants: TextView by bindView(R.id.participants)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(data[it])
                }
            }

            newMessages.setBackgroundColor(ContextCompat.getColor(newMessages.context, R.color.colorAccent))
        }

        fun bind(item: LocalConference) {
            topic.text = item.topic
            time.text = item.date.convertToRelativeReadableTime(time.context)
            participants.text = participants.context.getQuantityString(R.plurals.fragment_conference_participants,
                item.participantAmount)

            if (item.localIsRead) {
                newMessages.visibility = View.GONE
            } else {
                newMessages.visibility = View.VISIBLE

                newMessages.setBadgeCount(item.unreadMessageAmount)
            }

            if (item.image.isBlank()) {
                val icon = IconicsDrawable(image.context)
                    .sizeDp(96)
                    .paddingDp(16)
                    .colorRes(image.context, R.color.colorAccent)

                if (item.isGroup) {
                    icon.icon(CommunityMaterial.Icon.cmd_account_multiple)
                } else {
                    icon.icon(CommunityMaterial.Icon.cmd_account)
                }

                image.setImageDrawable(icon)
            } else {
                glide?.load(ProxerUrls.userImage(item.image).toString())
                    ?.transition(DrawableTransitionOptions.withCrossFade())
                    ?.circleCrop()
                    ?.into(image)
            }
        }
    }
}
