package me.proxer.app.chat.pub.message

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.longClicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindOptionalView
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.chat.pub.message.ChatAdapter.MessageViewHolder
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.getSafeParcelable
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import org.jetbrains.anko.dip
import java.util.concurrent.Callable

/**
 * @author Ruben Gees
 */
class ChatAdapter(savedInstanceState: Bundle?) : BaseAdapter<ParsedChatMessage, MessageViewHolder>() {

    private companion object {
        private const val IS_SELECTING_STATE = "chat_is_selecting"
        private const val TIME_DISPLAY_STATE = "chat_time_display"
        private const val MESSAGE_SELECTION_STATE = "chat_message_selection"
    }

    var glide: GlideRequests? = null
    val titleClickSubject: PublishSubject<Pair<ImageView, ParsedChatMessage>> = PublishSubject.create()
    val messageSelectionSubject: PublishSubject<Int> = PublishSubject.create()
    val linkClickSubject: PublishSubject<HttpUrl> = PublishSubject.create()
    val linkLongClickSubject: PublishSubject<HttpUrl> = PublishSubject.create()
    val mentionsClickSubject: PublishSubject<String> = PublishSubject.create()

    val selectedMessages: List<ParsedChatMessage>
        get() = data.filter { messageSelectionMap[it.id] == true }.sortedBy { it.date }

    val enqueuedMessageCount: Int
        get() = data.takeWhile { it.id.toLong() < 0 }.size

    private var layoutManager: RecyclerView.LayoutManager? = null

    private val messageSelectionMap: ParcelableStringBooleanMap
    private val timeDisplayMap: ParcelableStringBooleanMap

    private var isSelecting = false

    private val user by lazy { StorageHelper.user }

    init {
        messageSelectionMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getSafeParcelable(IS_SELECTING_STATE)
        }

        timeDisplayMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getSafeParcelable(TIME_DISPLAY_STATE)
        }

        isSelecting = savedInstanceState?.getBoolean(MESSAGE_SELECTION_STATE) == true

        setHasStableIds(true)
    }

    override fun getItemViewType(position: Int): Int {
        val current = data[position]
        var result: Int

        when {
            position - 1 < 0 -> result = if (position + 1 >= itemCount) {
                MessageType.SINGLE.type // The item is the only one.
            } else {
                val next = data[position + 1]

                if (next.userId == current.userId) {
                    MessageType.BOTTOM.type /* The item is the bottommost item and has an item from the same
                                                   user above. */
                } else {
                    MessageType.SINGLE.type /* The item is the bottommost item and doesn't have an item from
                                                   the same user above. */
                }
            }
            position + 1 >= itemCount -> {
                val previous = data[position - 1]

                result = if (previous.userId == current.userId) {
                    MessageType.TOP.type // The item is the topmost item and has an item from the same user beneath.
                } else {
                    MessageType.SINGLE.type /* The item is the topmost item and doesn't have an item from the
                                                   same user beneath. */
                }
            }
            else -> {
                val previous = data[position - 1]
                val next = data[position + 1]

                result = if (previous.userId == current.userId) {
                    if (next.userId == current.userId) {
                        MessageType.INNER.type // The item is in between two other items from the same user.
                    } else {
                        MessageType.TOP.type // The item has an item from the same user beneath but not above.
                    }
                } else {
                    if (next.userId == current.userId) {
                        MessageType.BOTTOM.type // The item has an item from the same user above but not beneath.
                    } else {
                        MessageType.SINGLE.type // The item stands alone.
                    }
                }
            }
        }

        if (current.userId == user?.id) {
            result += 4 // Make the item a "self" item.
        }

        return result
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (MessageType.from(viewType)) {
            MessageType.TOP, MessageType.SINGLE -> MessageTitleViewHolder(inflater
                .inflate(R.layout.item_message_single, parent, false))
            MessageType.BOTTOM, MessageType.INNER -> MessageViewHolder(inflater
                .inflate(R.layout.item_message, parent, false))
            else -> MessageViewHolder(inflater
                .inflate(R.layout.item_message_self, parent, false))
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val margins = getMarginsForPosition(position)
        val context = holder.itemView.context

        holder.bind(data[position], context.dip(margins.first), context.dip(margins.second))
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        layoutManager = recyclerView.layoutManager
    }

    override fun swapDataAndNotifyWithDiffing(newData: List<ParsedChatMessage>) {
        super.swapDataAndNotifyWithDiffing(newData)

        if (newData.isEmpty()) {
            messageSelectionMap.clear()
            timeDisplayMap.clear()
        } else {
            messageSelectionMap.entries.iterator().let { iterator ->
                while (iterator.hasNext()) {
                    val nextKey = iterator.next().key

                    if (newData.none { it.id == nextKey }) {
                        iterator.remove()
                    }
                }
            }
        }

        if (messageSelectionMap.size <= 0) {
            isSelecting = false
        }

        messageSelectionSubject.onNext(messageSelectionMap.size)
    }

    override fun onViewRecycled(holder: MessageViewHolder) {
        if (holder is MessageTitleViewHolder) {
            glide?.clear(holder.image)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        layoutManager = null
        glide = null
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(IS_SELECTING_STATE, messageSelectionMap)
        outState.putParcelable(TIME_DISPLAY_STATE, timeDisplayMap)
        outState.putBoolean(MESSAGE_SELECTION_STATE, isSelecting)
    }

    fun clearSelection() {
        isSelecting = false
        messageSelectionMap.clear()
    }

    private fun getMarginsForPosition(position: Int): Pair<Int, Int> {
        val marginTop: Int
        val marginBottom: Int

        when (MessageType.from(getItemViewType(position))) {
            MessageType.INNER, MessageType.SELF_INNER -> {
                marginTop = 0
                marginBottom = 0
            }
            MessageType.SINGLE, MessageType.SELF_SINGLE -> {
                marginTop = 6
                marginBottom = 6
            }
            MessageType.TOP, MessageType.SELF_TOP -> {
                marginTop = 6
                marginBottom = 0
            }
            MessageType.BOTTOM, MessageType.SELF_BOTTOM -> {
                marginTop = 0
                marginBottom = 6
            }
        }

        return marginTop to if (position == 0) 0 else marginBottom
    }

    open inner class MessageViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val root: ViewGroup by bindView(R.id.root)
        internal val container: CardView by bindView(R.id.container)
        internal val text: BBCodeView by bindView(R.id.text)
        internal val time: TextView by bindView(R.id.time)
        internal val sendStatus: ImageView? by bindOptionalView(R.id.sendStatus)

        init {
            sendStatus?.setImageDrawable(IconicsDrawable(text.context, CommunityMaterial.Icon.cmd_clock)
                .sizeDp(16)
                .iconColor(text.context))
        }

        internal open fun bind(message: ParsedChatMessage, marginTop: Int, marginBottom: Int) {
            root.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe { onContainerClick(root, it) }

            root.longClicks(Callable { onContainerLongClickHandled(root) })
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe { onContainerLongClick(root, it) }

            applyMessage(message)
            applyTime(message)
            applySendStatus(message)
            applySelection(message)
            applyTimeVisibility(message)
            applyMargins(marginTop, marginBottom)
        }

        internal open fun onContainerClick(v: View, message: ParsedChatMessage) {
            if (isSelecting) {
                messageSelectionMap.putOrRemove(message.id)

                if (messageSelectionMap.size <= 0) {
                    isSelecting = false
                }

                messageSelectionSubject.onNext(messageSelectionMap.size)
            } else {
                timeDisplayMap.putOrRemove(message.id)
            }

            applySelection(message)
            applyTimeVisibility(message)

            layoutManager?.requestSimpleAnimationsInNextLayout()
        }

        internal open fun onContainerLongClick(v: View, message: ParsedChatMessage) {
            if (!isSelecting) {
                isSelecting = true

                messageSelectionMap.put(message.id, true)
                messageSelectionSubject.onNext(messageSelectionMap.size)

                applySelection(message)
            }
        }

        internal open fun onContainerLongClickHandled(v: View): Boolean {
            return !isSelecting
        }

        internal open fun applyMessage(message: ParsedChatMessage) {
            text.tree = message.styledMessage
        }

        internal open fun applyTime(message: ParsedChatMessage) {
            time.text = message.date.convertToRelativeReadableTime(time.context)
        }

        internal open fun applySendStatus(message: ParsedChatMessage) = when (message.id.toLong() < 0) {
            true -> sendStatus?.visibility = View.VISIBLE
            false -> sendStatus?.visibility = View.GONE
        }

        internal open fun applySelection(message: ParsedChatMessage) {
            container.setCardBackgroundColor(ContextCompat.getColorStateList(container.context, when {
                messageSelectionMap[message.id] == true -> R.color.selected
                else -> R.color.card_background
            }))
        }

        internal open fun applyTimeVisibility(message: ParsedChatMessage) {
            time.visibility = when (timeDisplayMap[message.id]) {
                true -> View.VISIBLE
                else -> View.GONE
            }
        }

        internal open fun applyMargins(marginTop: Int, marginBottom: Int) {
            (root.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin = marginTop
                bottomMargin = marginBottom
            }
        }
    }

    internal inner class MessageTitleViewHolder(itemView: View) : MessageViewHolder(itemView) {

        internal val titleContainer: ViewGroup by bindView(R.id.titleContainer)
        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)

        override fun bind(message: ParsedChatMessage, marginTop: Int, marginBottom: Int) {
            super.bind(message, marginTop, marginBottom)

            titleContainer.clicks()
                .mapAdapterPosition({ adapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(titleClickSubject)

            ViewCompat.setTransitionName(image, "chat_${message.id}")

            title.text = message.username
            title.requestLayout()

            if (message.image.isBlank()) {
                image.setIconicsImage(CommunityMaterial.Icon.cmd_account, 32, 4, R.color.colorAccent)
            } else {
                glide?.load(ProxerUrls.userImage(message.image).toString())
                    ?.transition(DrawableTransitionOptions.withCrossFade())
                    ?.circleCrop()
                    ?.into(image)
            }
        }
    }

    private enum class MessageType(val type: Int) {
        INNER(0), SINGLE(1), TOP(2), BOTTOM(3),
        SELF_INNER(4), SELF_SINGLE(5), SELF_TOP(6), SELF_BOTTOM(7);

        companion object {
            fun from(type: Int) = values().firstOrNull { it.type == type }
                ?: throw IllegalArgumentException("Unknown type: $type")
        }
    }
}
