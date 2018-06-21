package me.proxer.app.chat.prv.conference

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
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
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.conference.ConferenceAdapter.ViewHolder
import me.proxer.app.util.extension.colorRes
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.toAppString
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
class ConferenceAdapter : BaseAdapter<ConferenceWithMessage, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<ConferenceWithMessage> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = data[position].conference.id

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

    override fun areItemsTheSame(old: ConferenceWithMessage, new: ConferenceWithMessage): Boolean {
        return old.conference.id == new.conference.id
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val image: ImageView by bindView(R.id.image)
        internal val topic: TextView by bindView(R.id.topic)
        internal val time: TextView by bindView(R.id.time)
        internal val previewTextContainer: ViewGroup by bindView(R.id.previewTextContainer)
        internal val previewText: TextView by bindView(R.id.previewText)
        internal val newMessages: MaterialBadgeTextView by bindView(R.id.newMessages)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(data[it])
                }
            }

            newMessages.setBackgroundColor(ContextCompat.getColor(newMessages.context, R.color.colorAccent))
        }

        fun bind(item: ConferenceWithMessage) {
            bindTopic(item)
            bindTime(item)
            bindPreviewText(item)
            bindNewMessages(item)
            bindImage(item)
        }

        private fun bindTopic(item: ConferenceWithMessage) {
            val (conference, firstMessageText, firstMessageUsername, firstMessageAction) = item

            topic.text = conference.topic

            if (firstMessageText != null && firstMessageUsername != null && firstMessageAction != null) {

                (topic.layoutParams as RelativeLayout.LayoutParams).apply {
                    bottomMargin = 0

                    addRule(RelativeLayout.CENTER_VERTICAL, 0)
                }
            } else {
                (topic.layoutParams as RelativeLayout.LayoutParams).apply {
                    bottomMargin = previewTextContainer.context.dip(8)

                    addRule(RelativeLayout.CENTER_VERTICAL)
                }
            }
        }

        private fun bindTime(item: ConferenceWithMessage) {
            time.text = item.conference.date.convertToRelativeReadableTime(time.context)
        }

        private fun bindPreviewText(item: ConferenceWithMessage) {
            val (conference, firstMessageText, firstMessageUsername, firstMessageAction) = item

            if (firstMessageText != null && firstMessageUsername != null && firstMessageAction != null) {
                val trimmedFirstMessageText = firstMessageText
                    .replace("\r\n", " ")
                    .replace("\n", " ")
                    .trim()

                val processedFirstMessageText = if (conference.isGroup) {
                    "$firstMessageUsername: $trimmedFirstMessageText"
                } else {
                    trimmedFirstMessageText
                }

                previewText.text = firstMessageAction.toAppString(previewText.context,
                    firstMessageUsername, processedFirstMessageText)
            } else {
                previewText.text = null
            }

            if (conference.localIsRead) {
                (previewTextContainer.layoutParams as RelativeLayout.LayoutParams).apply {
                    topMargin = previewTextContainer.context.dip(8)
                    bottomMargin = previewTextContainer.context.dip(8)

                    addRule(RelativeLayout.ALIGN_TOP, 0)
                    addRule(RelativeLayout.ALIGN_BOTTOM, 0)
                    addRule(RelativeLayout.BELOW, R.id.topic)
                }
            } else {
                (previewTextContainer.layoutParams as RelativeLayout.LayoutParams).apply {
                    topMargin = 0
                    bottomMargin = 0

                    addRule(RelativeLayout.ALIGN_TOP, R.id.newMessages)
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.newMessages)
                    addRule(RelativeLayout.BELOW, 0)
                }
            }
        }

        private fun bindNewMessages(item: ConferenceWithMessage) {
            if (item.conference.localIsRead) {
                newMessages.visibility = View.GONE
            } else {
                newMessages.visibility = View.VISIBLE

                newMessages.setBadgeCount(item.conference.unreadMessageAmount)
            }
        }

        private fun bindImage(item: ConferenceWithMessage) {
            if (item.conference.image.isBlank()) {
                val icon = IconicsDrawable(image.context)
                    .sizeDp(96)
                    .paddingDp(16)
                    .colorRes(image.context, R.color.colorAccent)

                if (item.conference.isGroup) {
                    icon.icon(CommunityMaterial.Icon.cmd_account_multiple)
                } else {
                    icon.icon(CommunityMaterial.Icon.cmd_account)
                }

                image.setImageDrawable(icon)
            } else {
                glide?.load(ProxerUrls.userImage(item.conference.image).toString())
                    ?.transition(DrawableTransitionOptions.withCrossFade())
                    ?.circleCrop()
                    ?.into(image)
            }
        }
    }
}
