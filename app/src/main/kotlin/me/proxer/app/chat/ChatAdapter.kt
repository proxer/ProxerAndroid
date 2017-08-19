package me.proxer.app.chat

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.chat.ChatAdapter.MessageViewHolder
import me.proxer.app.util.Utils
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.library.enums.MessageAction
import okhttp3.HttpUrl
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
class ChatAdapter(savedInstanceState: Bundle?, private val isGroup: Boolean)
    : BaseAdapter<LocalMessage, MessageViewHolder>() {

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
        get() = data.filter { messageSelectionMap[it.id.toString()] == true }.sortedBy { it.date }

    private val messageSelectionMap: ParcelableStringBooleanMap
    private val timeDisplayMap: ParcelableStringBooleanMap

    private var isSelecting = false
    private val user by lazy { StorageHelper.user }

    init {
        messageSelectionMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(IS_SELECTING_STATE)
        }

        timeDisplayMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(TIME_DISPLAY_STATE)
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
                        MessageType.BOTTOM.type // The item is the bottommost item and has an item from the same user above.
                    } else {
                        MessageType.SINGLE.type // The item is the bottommost item and doesn't have an item from the same user above.
                    }
                }
                position + 1 >= itemCount -> {
                    val previous = data[position - 1]

                    result = if (previous.userId == current.userId && previous.action == MessageAction.NONE) {
                        MessageType.TOP.type // The item is the topmost item and has an item from the same user beneath.
                    } else {
                        MessageType.SINGLE.type // The item is the topmost item and doesn't have an item from the same user beneath.
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
                            MessageType.SINGLE.type  // The item stands alone.
                        }
                    }
                }
            }

            if (current.userId == user?.id) {
                result += 4 // Make the item a "self" item.
            }
        }

        return result
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (MessageType.from(viewType)) {
            MessageType.TOP, MessageType.SINGLE -> {
                if (isGroup) {
                    MessageTitleViewHolder(inflater.inflate(R.layout.item_message_single, parent, false))
                } else {
                    MessageViewHolder(inflater.inflate(R.layout.item_message, parent, false))
                }
            }
            MessageType.BOTTOM, MessageType.INNER -> {
                MessageViewHolder(inflater.inflate(R.layout.item_message, parent, false))
            }
            MessageType.ACTION -> {
                ActionViewHolder(inflater.inflate(R.layout.item_message_action, parent, false))
            }
            else -> MessageViewHolder(inflater.inflate(R.layout.item_message_self, parent, false))
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val margins = getMarginsForPosition(position)
        val context = holder.itemView.context

        holder.bind(data[position], context.dip(margins.first), context.dip(margins.second))
    }

    override fun areItemsTheSame(old: LocalMessage, new: LocalMessage) = old.id == new.id

    override fun areContentsTheSame(old: LocalMessage, new: LocalMessage) = old.userId == new.userId
            && old.action == new.action && old.date == new.date && old.message == new.message

    override fun swapData(newData: List<LocalMessage>) {
        super.swapData(newData)

        messageSelectionSubject.onNext(messageSelectionMap.size)
    }

    override fun clear() {
        clearSelection()
        timeDisplayMap.clear()

        messageSelectionSubject.onNext(0)

        super.clear()
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(IS_SELECTING_STATE, messageSelectionMap)
        outState.putParcelable(TIME_DISPLAY_STATE, timeDisplayMap)
        outState.putBoolean(MESSAGE_SELECTION_STATE, isSelecting)
    }

    fun clearSelection() {
        isSelecting = false

        messageSelectionMap.clear()
        notifyDataSetChanged()
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

    open inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        protected val root: ViewGroup by bindView(R.id.root)
        protected val container: CardView by bindView(R.id.container)
        protected val text: TextView by bindView(R.id.text)
        protected val time: TextView by bindView(R.id.time)

        init {
            root.setOnClickListener { onContainerClick(it) }
            root.setOnLongClickListener { onContainerLongClick(it) }
            text.movementMethod = TouchableMovementMethod.getInstance()
            text.setTextColor(ContextCompat.getColor(text.context, R.color.textColorPrimary))
        }

        open fun bind(message: LocalMessage, marginTop: Int, marginBottom: Int) {
            applyMessage(message)
            applyTime(message)
            applySendStatus(message)
            applySelection(message)
            applyTimeVisibility(message)
            applyMargins(marginTop, marginBottom)
        }

        open protected fun onContainerClick(v: View) = withSafeAdapterPosition(this) {
            val current = data[it]
            val id = current.id.toString()

            if (isSelecting) {
                if (messageSelectionMap[id] == true) {
                    messageSelectionMap.remove(id)

                    if (messageSelectionMap.size <= 0) {
                        isSelecting = false
                    }
                } else {
                    messageSelectionMap.put(id, true)
                }

                messageSelectionSubject.onNext(messageSelectionMap.size)
            } else {
                if (timeDisplayMap[id] == true) {
                    timeDisplayMap.remove(id)
                } else {
                    timeDisplayMap.put(id, true)
                }
            }

            notifyDataSetChanged()
        }

        open protected fun onContainerLongClick(v: View): Boolean {
            var consumed = false

            withSafeAdapterPosition(this) {
                val current = data[it]
                val id = current.id.toString()

                if (!isSelecting) {
                    isSelecting = true
                    messageSelectionMap.put(id, true)

                    messageSelectionSubject.onNext(messageSelectionMap.size)
                    notifyDataSetChanged()

                    consumed = true
                }
            }

            return consumed
        }

        protected open fun applyMessage(message: LocalMessage) {
            text.text = Utils.buildClickableText(text.context, message.message.trim(),
                    onWebClickListener = Link.OnClickListener {
                        linkClickSubject.onNext(Utils.parseAndFixUrl(it))
                    },
                    onWebLongClickListener = Link.OnLongClickListener {
                        linkLongClickSubject.onNext(Utils.parseAndFixUrl(it))
                    },
                    onMentionsClickListener = Link.OnClickListener {
                        mentionsClickSubject.onNext(it.trim().substring(1))
                    })
        }

        protected open fun applyTime(message: LocalMessage) {
            time.text = message.date.convertToRelativeReadableTime(time.context)
        }

        protected open fun applySendStatus(message: LocalMessage) = if (message.id < 0) {
            text.setCompoundDrawablesWithIntrinsicBounds(null, null, IconicsDrawable(text.context)
                    .icon(CommunityMaterial.Icon.cmd_clock)
                    .sizeDp(24)
                    .paddingDp(4)
                    .colorRes(R.color.icon), null)
        } else {
            text.setCompoundDrawables(null, null, null, null)
        }

        protected open fun applySelection(message: LocalMessage) {
            container.cardBackgroundColor = ContextCompat.getColorStateList(container.context, when {
                messageSelectionMap[message.id.toString()] == true -> R.color.selected
                else -> R.color.card_background
            })
        }

        protected open fun applyTimeVisibility(message: LocalMessage) {
            time.visibility = when (timeDisplayMap[message.id.toString()]) {
                true -> View.VISIBLE
                else -> View.GONE
            }
        }

        protected open fun applyMargins(marginTop: Int, marginBottom: Int) {
            (root.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin = marginTop
                bottomMargin = marginBottom
            }
        }
    }

    internal inner class MessageTitleViewHolder(itemView: View) : MessageViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)

        init {
            title.setOnClickListener {
                withSafeAdapterPosition(this) {
                    titleClickSubject.onNext(data[it])
                }
            }
        }

        override fun bind(message: LocalMessage, marginTop: Int, marginBottom: Int) {
            super.bind(message, marginTop, marginBottom)

            title.text = message.username
        }
    }

    internal inner class ActionViewHolder(itemView: View) : MessageViewHolder(itemView) {

        override fun onContainerClick(v: View) = withSafeAdapterPosition(this) {
            val current = data[it]
            val id = current.id.toString()

            if (timeDisplayMap[id] == true) {
                time.visibility = View.GONE

                timeDisplayMap.remove(id)
            } else {
                time.visibility = View.VISIBLE

                timeDisplayMap.put(id, true)
            }
        }

        override fun onContainerLongClick(v: View) = false

        override fun applyMessage(message: LocalMessage) {
            text.text = Utils.buildClickableText(text.context, generateText(message),
                    onMentionsClickListener = Link.OnClickListener {
                        mentionsClickSubject.onNext(it.trim().substring(1))
                    })
        }

        private fun generateText(message: LocalMessage) = when (message.action) {
            MessageAction.ADD_USER -> text.context.getString(R.string.action_conference_add_user,
                    "@${message.username}", "@${message.message}")
            MessageAction.REMOVE_USER -> text.context.getString(R.string.action_conference_delete_user,
                    "@${message.username}", "@${message.message}")
            MessageAction.SET_LEADER -> text.context.getString(R.string.action_conference_set_leader,
                    "@${message.username}", "@${message.message}")
            MessageAction.SET_TOPIC -> text.context.getString(R.string.action_conference_set_topic,
                    "@${message.username}", message.message)
            MessageAction.NONE -> message.message
        }
    }

    private enum class MessageType(val type: Int) {
        INNER(0), SINGLE(1), TOP(2), BOTTOM(3),
        SELF_INNER(4), SELF_SINGLE(5), SELF_TOP(6), SELF_BOTTOM(7),
        ACTION(8);

        companion object {
            fun from(type: Int) = values().firstOrNull { it.type == type }
                    ?: throw IllegalArgumentException("Unknown type: $type")
        }
    }
}
