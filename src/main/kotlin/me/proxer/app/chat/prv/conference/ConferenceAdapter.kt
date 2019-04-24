package me.proxer.app.chat.prv.conference

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.chip.Chip
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.toIconicsSizeDp
import com.mikepenz.iconics.utils.toIconicsSizePx
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.conference.ConferenceAdapter.ViewHolder
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.logErrors
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.sp
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toIconicsColorAttr
import me.proxer.app.util.extension.toLocalDateTime
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class ConferenceAdapter(private val storageHelper: StorageHelper) : BaseAdapter<ConferenceWithMessage, ViewHolder>() {

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

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val container: ViewGroup by bindView(R.id.container)
        internal val image: ImageView by bindView(R.id.image)
        internal val topic: TextView by bindView(R.id.topic)
        internal val time: TextView by bindView(R.id.time)
        internal val previewTextContainer: ViewGroup by bindView(R.id.previewTextContainer)
        internal val previewText: TextView by bindView(R.id.previewText)
        internal val newMessages: Chip by bindView(R.id.newMessages)

        fun bind(item: ConferenceWithMessage) {
            container.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            bindTopic(item)
            bindTime(item)
            bindPreviewText(item)
            bindNewMessages(item)
            bindImage(item)
        }

        private fun bindTopic(item: ConferenceWithMessage) {
            topic.text = item.conference.topic

            if (item.message != null) {
                topic.updateLayoutParams<RelativeLayout.LayoutParams> {
                    bottomMargin = 0

                    addRule(RelativeLayout.CENTER_VERTICAL, 0)
                }
            } else {
                topic.updateLayoutParams<RelativeLayout.LayoutParams> {
                    bottomMargin = previewTextContainer.context.dip(8)

                    addRule(RelativeLayout.CENTER_VERTICAL)
                }
            }
        }

        private fun bindTime(item: ConferenceWithMessage) {
            time.text = item.conference.date.toLocalDateTime().distanceInWordsToNow(time.context)
        }

        private fun bindPreviewText(item: ConferenceWithMessage) {
            if (item.message != null) {
                val messageFromUser = item.message.userId == storageHelper.user?.id

                val trimmedFirstMessageText = item.message.messageText
                    .replace("\r\n", " ")
                    .replace("\n", " ")
                    .trim()

                val processedFirstMessageText = if (item.conference.isGroup && !messageFromUser) {
                    "${item.message.username}: $trimmedFirstMessageText"
                } else {
                    trimmedFirstMessageText
                }

                val icon = when (messageFromUser) {
                    true -> when (item.message.messageId < 0) {
                        true -> CommunityMaterial.Icon.cmd_clock_outline
                        false -> CommunityMaterial.Icon.cmd_check
                    }
                    false -> null
                }

                val iconicsIcon = if (icon == null) null else generateMessageStatusDrawable(previewText.context, icon)

                previewText.text = item.message.messageAction.toAppString(
                    previewText.context, item.message.username, processedFirstMessageText
                )

                previewText.setCompoundDrawables(iconicsIcon, null, null, null)
            } else {
                previewText.text = null
                previewText.setCompoundDrawables(null, null, null, null)
            }

            if (item.conference.localIsRead) {
                previewTextContainer.updateLayoutParams<RelativeLayout.LayoutParams> {
                    topMargin = previewTextContainer.context.dip(8)
                    bottomMargin = previewTextContainer.context.dip(8)

                    addRule(RelativeLayout.ALIGN_TOP, 0)
                    addRule(RelativeLayout.ALIGN_BOTTOM, 0)
                    addRule(RelativeLayout.BELOW, R.id.topic)
                }
            } else {
                previewTextContainer.updateLayoutParams<RelativeLayout.LayoutParams> {
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
                newMessages.isGone = true
            } else {
                newMessages.isVisible = true
                newMessages.text = item.conference.unreadMessageAmount.toString()
            }
        }

        private fun bindImage(item: ConferenceWithMessage) {
            if (item.conference.image.isBlank()) {
                val icon = IconicsDrawable(image.context)
                    .size(96.toIconicsSizeDp())
                    .padding(16.toIconicsSizeDp())
                    .color(R.attr.colorSecondary.toIconicsColorAttr(image.context))

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
                    ?.logErrors()
                    ?.into(image)
            }
        }

        private fun generateMessageStatusDrawable(context: Context, icon: IIcon) = IconicsDrawable(context)
            .icon(icon)
            .iconColor(context)
            .size(context.sp(14).toIconicsSizePx())
    }
}
