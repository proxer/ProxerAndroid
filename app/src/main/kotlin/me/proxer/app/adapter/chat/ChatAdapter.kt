package me.proxer.app.adapter.chat

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.entity.LocalUser
import me.proxer.app.entity.chat.LocalMessage
import me.proxer.app.util.ParcelableStringBooleanMap
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.bindView
import me.proxer.library.enums.MessageAction
import okhttp3.HttpUrl
import org.jetbrains.anko.collections.forEachReversedByIndex
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
class ChatAdapter(savedInstanceState: Bundle?, val isGroup: Boolean) : BaseAdapter<LocalMessage>() {

    private companion object {
        private const val SELECTED_STATE = "chat_selected"
        private const val SHOWING_TIME_STATE = "chat_showing_time"
        private const val SELECTING_STATE = "chat_selecting"
    }

    var user: LocalUser? = null
    var callback: ChatAdapterCallback? = null

    val selectedItems: List<LocalMessage>
        get() = internalList.filter { selected[it.localId.toString()] ?: false }.sortedBy { it.date }

    private val selected: ParcelableStringBooleanMap
    private val showingTime: ParcelableStringBooleanMap

    private var selecting = false

    init {
        selected = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(SELECTED_STATE)
        }

        showingTime = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(SHOWING_TIME_STATE)
        }

        selecting = savedInstanceState?.getBoolean(SELECTING_STATE) ?: false

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<LocalMessage> {
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

    override fun onBindViewHolder(holder: BaseViewHolder<LocalMessage>, position: Int) {
        holder as MessageViewHolder

        val margins = getMarginsForPosition(position)
        val context = holder.itemView.context

        holder.bind(internalList[position], context.dip(margins.first), context.dip(margins.second))
    }

    override fun getItemId(position: Int): Long = internalList[position].id.toLong()

    override fun areItemsTheSame(oldItem: LocalMessage, newItem: LocalMessage) = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: LocalMessage, newItem: LocalMessage): Boolean {
        return oldItem.message == newItem.message && oldItem.userId == newItem.userId &&
                oldItem.action == newItem.action && oldItem.date == newItem.date
    }

    override fun getItemViewType(position: Int): Int {
        val current = internalList[position]
        var result: Int

        if (current.action != MessageAction.NONE) {
            result = MessageType.ACTION.type
        } else {
            if (position - 1 < 0) {
                if (position + 1 >= itemCount) {
                    result = MessageType.SINGLE.type // The item is the only one
                } else {
                    val next = internalList[position + 1]

                    if (next.userId == current.userId && next.action == MessageAction.NONE) {
                        result = MessageType.BOTTOM.type // The item is the bottommost item and has an item from the same user above
                    } else {
                        result = MessageType.SINGLE.type // The item is the bottommost item and doesn't have an item from the same user above
                    }
                }
            } else if (position + 1 >= itemCount) {
                val previous = internalList[position - 1]

                if (previous.userId == current.userId && previous.action == MessageAction.NONE) {
                    result = MessageType.TOP.type // The item is the topmost item and has an item from the same user beneath
                } else {
                    result = MessageType.SINGLE.type // The item is the topmost item and doesn't have an item from the same user beneath
                }
            } else {
                val previous = internalList[position - 1]
                val next = internalList[position + 1]

                if (previous.userId == current.userId && previous.action == MessageAction.NONE) {
                    if (next.userId == current.userId && next.action == MessageAction.NONE) {
                        result = MessageType.INNER.type // The item is in between two other items from the same user
                    } else {
                        result = MessageType.TOP.type // The item has an item from the same user beneath but not above
                    }
                } else {
                    if (next.userId == current.userId && next.action == MessageAction.NONE) {
                        result = MessageType.BOTTOM.type // The item has an item from the same user above but not beneath
                    } else {
                        result = MessageType.SINGLE.type  // The item stands alone
                    }
                }
            }

            if (current.userId == user?.id) {
                result += 4 // Make the item a "self" item
            }
        }

        return result
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    @Synchronized
    override fun insert(items: Iterable<LocalMessage>) {
        val currentItems = items.toMutableList()
        val synchronizedItems = mutableListOf<LocalMessage>()
        val nonSynchronizedItems = mutableListOf<LocalMessage>()

        internalList.forEachReversedByIndex {
            if (it.id.toLong() >= 0) {
                synchronizedItems += it
            } else {
                val existingItemPosition = currentItems.indexOfFirst { new ->
                    it.userId == new.userId && it.action == new.action && it.message == new.message
                }

                if (existingItemPosition >= 0) {
                    currentItems.removeAt(existingItemPosition)
                } else {
                    nonSynchronizedItems += it
                }
            }
        }

        items.forEach {
            if (it.id.toLong() >= 0) {
                synchronizedItems += it
            } else {
                nonSynchronizedItems += it
            }
        }

        val test2 = 2
        val test = test2

        doUpdates(nonSynchronizedItems.sortedBy { it.id.toLong() }
                .plus(synchronizedItems.sortedByDescending { it.id.toLong() }))
    }

    override fun clear() {
        super.clear()

        showingTime.clear()
        clearSelection()

        if (selected.size > 0) {
            callback?.onMessageSelection(0)
        }
    }

    fun clearSelection() {
        selecting = false

        selected.clear()
        notifyDataSetChanged()
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(SELECTED_STATE, selected)
        outState.putParcelable(SHOWING_TIME_STATE, showingTime)
        outState.putBoolean(SELECTING_STATE, selecting)
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

    interface ChatAdapterCallback {
        fun onMessageTitleClick(message: LocalMessage) {}
        fun onMessageSelection(count: Int) {}
        fun onMessageLinkClick(link: HttpUrl) {}
        fun onMessageLinkLongClick(link: HttpUrl) {}
        fun onMentionsClick(username: String) {}
    }

    open internal inner class MessageViewHolder(itemView: View) : BaseViewHolder<LocalMessage>(itemView) {

        protected val root: ViewGroup by bindView(R.id.root)
        protected val container: CardView by bindView(R.id.container)
        protected val text: TextView by bindView(R.id.text)
        protected val time: TextView by bindView(R.id.time)

        init {
            root.setOnClickListener { onContainerClick(it) }
            root.setOnLongClickListener { onContainerLongClick(it) }
            text.movementMethod = TouchableMovementMethod.getInstance()
        }

        open fun bind(message: LocalMessage, marginTop: Int, marginBottom: Int) {
            applyMessage(message)
            applyTime(message)
            applySendStatus(message)
            applySelection(message)
            applyTimeVisibility(message)
            applyMargins(marginTop, marginBottom)
        }

        open protected fun onContainerClick(v: View) {
            withSafeAdapterPosition {
                val current = internalList[it]
                val id = current.localId.toString()

                if (selecting) {
                    if (selected[id] ?: false) {
                        selected.remove(id)

                        if (selected.size <= 0) {
                            selecting = false
                        }
                    } else {
                        selected.put(id, true)
                    }

                    callback?.onMessageSelection(selected.size)
                } else {
                    if (showingTime[id] ?: false) {
                        showingTime.remove(id)
                    } else {
                        showingTime.put(id, true)
                    }
                }

                notifyDataSetChanged()
            }
        }

        open protected fun onContainerLongClick(v: View): Boolean {
            var consumed = false

            withSafeAdapterPosition {
                val current = internalList[it]
                val id = current.localId.toString()

                if (!selecting) {
                    selecting = true
                    selected.put(id, true)

                    notifyDataSetChanged()
                    callback?.onMessageSelection(selected.size)

                    consumed = true
                }
            }

            return consumed
        }

        protected open fun applyMessage(message: LocalMessage) {
            text.text = Utils.buildClickableText(text.context, message.message.trim(),
                    onWebClickListener = Link.OnClickListener {
                        callback?.onMessageLinkClick(Utils.parseAndFixUrl(it))
                    },
                    onWebLongClickListener = Link.OnLongClickListener {
                        callback?.onMessageLinkLongClick(Utils.parseAndFixUrl(it))
                    },
                    onMentionsClickListener = Link.OnClickListener {
                        callback?.onMentionsClick(it.trim().substring(1))
                    })
        }

        protected open fun applyTime(message: LocalMessage) {
            time.text = TimeUtils.convertToRelativeReadableTime(time.context, message.date)
        }

        protected open fun applySendStatus(message: LocalMessage) {
            if (message.id.toLong() < 0) {
                text.setCompoundDrawablesWithIntrinsicBounds(null, null, IconicsDrawable(text.context)
                        .icon(CommunityMaterial.Icon.cmd_clock)
                        .sizeDp(24)
                        .paddingDp(4)
                        .colorRes(R.color.icon), null)
            } else {
                text.setCompoundDrawables(null, null, null, null)
            }
        }

        protected open fun applySelection(message: LocalMessage) {
            container.cardBackgroundColor = ContextCompat.getColorStateList(container.context, when {
                selected[message.localId.toString()] ?: false -> R.color.selected
                else -> R.color.card_background
            })
        }

        protected open fun applyTimeVisibility(message: LocalMessage) {
            if (showingTime[message.localId.toString()] ?: false) {
                time.visibility = View.VISIBLE
            } else {
                time.visibility = View.GONE
            }
        }

        protected open fun applyMargins(marginTop: Int, marginBottom: Int) {
            val params = root.layoutParams as ViewGroup.MarginLayoutParams

            params.topMargin = marginTop
            params.bottomMargin = marginBottom

            root.layoutParams = params
        }
    }

    internal inner class MessageTitleViewHolder(itemView: View) : MessageViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)

        init {
            title.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onMessageTitleClick(internalList[it])
                }
            }
        }

        override fun bind(message: LocalMessage, marginTop: Int, marginBottom: Int) {
            super.bind(message, marginTop, marginBottom)

            title.text = message.username
        }
    }

    internal inner class ActionViewHolder(itemView: View) : MessageViewHolder(itemView) {

        override fun onContainerClick(v: View) {
            withSafeAdapterPosition {
                val current = internalList[it]
                val id = current.localId.toString()

                if (showingTime[id] ?: false) {
                    time.visibility = View.GONE

                    showingTime.remove(id)
                } else {
                    time.visibility = View.VISIBLE

                    showingTime.put(id, true)
                }
            }
        }

        override fun onContainerLongClick(v: View): Boolean {
            return false
        }

        override fun applyMessage(message: LocalMessage) {
            text.text = Utils.buildClickableText(text.context, generateText(message),
                    onMentionsClickListener = Link.OnClickListener {
                        callback?.onMentionsClick(it.trim().substring(1))
                    })
        }

        private fun generateText(message: LocalMessage): String {
            return when (message.action) {
                MessageAction.ADD_USER -> text.context.getString(R.string.action_conference_add_user,
                        "@${message.username}", "@${message.message}")
                MessageAction.REMOVE_USER -> text.context.getString(R.string.action_conference_remove_user,
                        "@${message.username}", "@${message.message}")
                MessageAction.SET_LEADER -> text.context.getString(R.string.action_conference_set_leader,
                        "@${message.username}", "@${message.message}")
                MessageAction.SET_TOPIC -> text.context.getString(R.string.action_conference_set_topic,
                        "@${message.username}", message.message)
                MessageAction.NONE -> message.message
            }
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
