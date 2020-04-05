package me.proxer.app.chat.prv.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindOptionalView
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.prv.message.MessengerAdapter.MessageViewHolder
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.toSimpleBBTree
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.getSafeParcelable
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toLocalDateTime
import me.proxer.library.enums.MessageAction
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class MessengerAdapter(
    savedInstanceState: Bundle?,
    private val isGroup: Boolean,
    private val storageHelper: StorageHelper
) : BaseAdapter<LocalMessage, MessageViewHolder>() {

    private companion object {
        private const val IS_SELECTING_STATE = "chat_is_selecting"
        private const val TIME_DISPLAY_STATE = "chat_time_display"
        private const val MESSAGE_SELECTION_STATE = "chat_message_selection"
    }

    val titleClickSubject: PublishSubject<LocalMessage> = PublishSubject.create()
    val messageSelectionSubject: PublishSubject<Int> = PublishSubject.create()
    val linkClickSubject: PublishSubject<HttpUrl> = PublishSubject.create()
    val linkLongClickSubject: PublishSubject<HttpUrl> = PublishSubject.create()
    val mentionsClickSubject: PublishSubject<String> = PublishSubject.create()

    val selectedMessages: List<LocalMessage>
        get() = data
            .asSequence()
            .filter { messageSelectionMap[it.id.toString()] == true }
            .sortedBy { it.date }
            .toList()

    val enqueuedMessageCount: Int
        get() = data.takeWhile { it.id < 0 }.size

    private var layoutManager: RecyclerView.LayoutManager? = null

    private val messageSelectionMap: ParcelableStringBooleanMap
    private val timeDisplayMap: ParcelableStringBooleanMap

    private var isSelecting = false

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

    override fun getItemId(position: Int): Long = data[position].id

    override fun getItemViewType(position: Int): Int {
        val current = data[position]
        var result: Int

        if (current.action != MessageAction.NONE) {
            result = MessageType.ACTION.type
        } else {
            when {
                position - 1 < 0 -> result = if (position + 1 >= itemCount) {
                    MessageType.SINGLE.type // The item is the only one.
                } else {
                    val next = data[position + 1]

                    if (next.userId == current.userId && next.action == MessageAction.NONE) {
                        MessageType.BOTTOM.type /* The item is the bottommost item and has an item from the same
                                                   user above. */
                    } else {
                        MessageType.SINGLE.type /* The item is the bottommost item and doesn't have an item from
                                                   the same user above. */
                    }
                }
                position + 1 >= itemCount -> {
                    val previous = data[position - 1]

                    result = if (previous.userId == current.userId && previous.action == MessageAction.NONE) {
                        MessageType.TOP.type // The item is the topmost item and has an item from the same user beneath.
                    } else {
                        MessageType.SINGLE.type /* The item is the topmost item and doesn't have an item from the
                                                   same user beneath. */
                    }
                }
                else -> {
                    val previous = data[position - 1]
                    val next = data[position + 1]

                    result = if (previous.userId == current.userId && previous.action == MessageAction.NONE) {
                        if (next.userId == current.userId && next.action == MessageAction.NONE) {
                            MessageType.INNER.type // The item is in between two other items from the same user.
                        } else {
                            MessageType.TOP.type // The item has an item from the same user beneath but not above.
                        }
                    } else {
                        if (next.userId == current.userId && next.action == MessageAction.NONE) {
                            MessageType.BOTTOM.type // The item has an item from the same user above but not beneath.
                        } else {
                            MessageType.SINGLE.type // The item stands alone.
                        }
                    }
                }
            }

            if (current.userId == storageHelper.user?.id) {
                result += 4 // Make the item a "self" item.
            }
        }

        return result
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (MessageType.from(viewType)) {
            MessageType.TOP, MessageType.SINGLE -> if (isGroup) {
                MessageTitleViewHolder(inflater.inflate(R.layout.item_message_single, parent, false))
            } else {
                MessageViewHolder(inflater.inflate(R.layout.item_message, parent, false))
            }
            MessageType.BOTTOM, MessageType.INNER -> MessageViewHolder(
                inflater.inflate(R.layout.item_message, parent, false)
            )
            MessageType.ACTION -> ActionViewHolder(
                inflater.inflate(R.layout.item_message_action, parent, false)
            )
            else -> MessageViewHolder(inflater.inflate(R.layout.item_message_self, parent, false))
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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        layoutManager = null
    }

    override fun swapDataAndNotifyWithDiffing(newData: List<LocalMessage>) {
        super.swapDataAndNotifyWithDiffing(newData)

        if (newData.isEmpty()) {
            messageSelectionMap.clear()
            timeDisplayMap.clear()
        } else {
            messageSelectionMap.entries.iterator().let { iterator ->
                while (iterator.hasNext()) {
                    val nextKey = iterator.next().key

                    if (newData.none { it.id.toString() == nextKey }) {
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

    override fun areItemsTheSame(old: LocalMessage, new: LocalMessage) = old.id == new.id

    override fun areContentsTheSame(old: LocalMessage, new: LocalMessage) = old.userId == new.userId &&
        old.action == new.action && old.date == new.date && old.message == new.message

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
            MessageType.ACTION -> {
                marginTop = 12
                marginBottom = 12
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
            sendStatus?.setImageDrawable(
                IconicsDrawable(text.context, CommunityMaterial.Icon3.cmd_clock_outline).apply {
                    colorInt = text.context.resolveColor(R.attr.colorIcon)
                    sizeDp = 15
                }
            )
        }

        internal open fun bind(message: LocalMessage, marginTop: Int, marginBottom: Int) {
            container.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe { onContainerClick(root, it) }

            container.longClicks { onContainerLongClickHandled(root) }
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe { onContainerLongClick(root, it) }

            applyMessage(message)
            applyTime(message)
            applySendStatus(message)
            applySelection(message)
            applyTimeVisibility(message)
            applyMargins(marginTop, marginBottom)
        }

        internal open fun onContainerClick(v: View, message: LocalMessage) {
            val id = message.id.toString()

            if (isSelecting) {
                messageSelectionMap.putOrRemove(id)

                if (messageSelectionMap.size <= 0) {
                    isSelecting = false
                }

                messageSelectionSubject.onNext(messageSelectionMap.size)
            } else {
                timeDisplayMap.putOrRemove(id)
            }

            applySelection(message)
            applyTimeVisibility(message)

            layoutManager?.requestSimpleAnimationsInNextLayout()
        }

        internal open fun onContainerLongClick(v: View, message: LocalMessage) {
            val id = message.id.toString()

            if (!isSelecting) {
                isSelecting = true

                messageSelectionMap.put(id, true)
                messageSelectionSubject.onNext(messageSelectionMap.size)

                applySelection(message)
            }
        }

        internal open fun onContainerLongClickHandled(v: View): Boolean {
            return !isSelecting
        }

        internal open fun applyMessage(message: LocalMessage) {
            text.tree = message.styledMessage
        }

        internal open fun applyTime(message: LocalMessage) {
            time.text = message.date.toLocalDateTime().distanceInWordsToNow(time.context)
        }

        internal open fun applySendStatus(message: LocalMessage) = when (message.id < 0) {
            true -> sendStatus?.isVisible = true
            false -> sendStatus?.isGone = true
        }

        internal open fun applySelection(message: LocalMessage) {
            container.setCardBackgroundColor(
                container.context.resolveColor(
                    when {
                        messageSelectionMap[message.id.toString()] == true -> R.attr.colorSelectedSurface
                        else -> R.attr.colorSurface
                    }
                )
            )
        }

        internal open fun applyTimeVisibility(message: LocalMessage) {
            time.isVisible = timeDisplayMap[message.id.toString()] == true
        }

        internal open fun applyMargins(marginTop: Int, marginBottom: Int) {
            root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = marginTop
                bottomMargin = marginBottom
            }
        }
    }

    internal inner class MessageTitleViewHolder(itemView: View) : MessageViewHolder(itemView) {

        internal val titleContainer: ViewGroup by bindView(R.id.titleContainer)
        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)

        init {
            // Messages in the private messages do not come with an avatar yet.
            image.isGone = true
        }

        override fun bind(message: LocalMessage, marginTop: Int, marginBottom: Int) {
            super.bind(message, marginTop, marginBottom)

            titleContainer.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe(titleClickSubject)

            title.text = message.username
            title.requestLayout()
        }
    }

    internal inner class ActionViewHolder(itemView: View) : MessageViewHolder(itemView) {

        override fun onContainerClick(v: View, message: LocalMessage) {
            val id = message.id.toString()

            timeDisplayMap.putOrRemove(id)

            time.isVisible = timeDisplayMap.containsKey(id)

            layoutManager?.requestSimpleAnimationsInNextLayout()
        }

        override fun onContainerLongClick(v: View, message: LocalMessage) = Unit

        override fun onContainerLongClickHandled(v: View) = false

        override fun applyMessage(message: LocalMessage) {
            val messageText = message.action.toAppString(text.context, message.username, message.message)

            text.tree = "[center]$messageText[/center]".toSimpleBBTree()
        }
    }

    private enum class MessageType(val type: Int) {
        INNER(0), SINGLE(1), TOP(2), BOTTOM(3),
        SELF_INNER(4), SELF_SINGLE(5), SELF_TOP(6), SELF_BOTTOM(7),
        ACTION(8);

        companion object {
            fun from(type: Int) = values().firstOrNull { it.type == type } ?: error("Unknown type: $type")
        }
    }
}
