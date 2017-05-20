package me.proxer.app.adapter.chat

import android.support.v4.content.ContextCompat
import android.support.v4.util.LongSparseArray
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
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.bindView
import me.proxer.library.enums.MessageAction
import okhttp3.HttpUrl
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
class ChatAdapter(val isGroup: Boolean) : BaseAdapter<LocalMessage>() {

    var user: LocalUser? = null

    private val selectedMap = LongSparseArray<Boolean>()
    private val showingTimeMap = LongSparseArray<Boolean>()

    var callback: ChatAdapterCallback? = null

    private var selecting = false

    val selectedItems: List<LocalMessage>
        get() = list.filter { selectedMap.get(it.localId, false) }.sortedBy { it.date }

    init {
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

    override fun getItemId(position: Int): Long = list[position].localId

    override fun areItemsTheSame(oldItem: LocalMessage, newItem: LocalMessage) = when {
        oldItem.id == "-1" && newItem.id != "-1" || oldItem.id != "-1" && newItem.id == "-1" -> {
            oldItem.message == newItem.message && oldItem.userId == newItem.userId && oldItem.action == newItem.action
        }
        else -> oldItem.localId == newItem.localId
    }

    override fun areContentsTheSame(oldItem: LocalMessage, newItem: LocalMessage): Boolean {
        return oldItem.message == newItem.message && oldItem.userId == newItem.userId &&
                oldItem.action == newItem.action && oldItem.date == newItem.date
    }

    override fun getItemViewType(position: Int): Int {
        var result: Int
        val current = internalList[position]

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

    override fun clear() {
        super.clear()

        showingTimeMap.clear()
        clearSelection()

        if (selectedMap.size() > 0) {
            callback?.onMessageSelection(0)
        }
    }

    fun clearSelection() {
        selecting = false

        selectedMap.clear()
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

        return marginTop to marginBottom
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
                val current = list[adapterPosition]

                if (selecting) {
                    if (selectedMap.get(current.localId, false)) {
                        selectedMap.remove(current.localId)

                        if (selectedMap.size() <= 0) {
                            selecting = false
                        }
                    } else {
                        selectedMap.put(current.localId, true)
                    }

                    callback?.onMessageSelection(selectedMap.size())
                } else {
                    if (showingTimeMap.get(current.localId, false)) {
                        showingTimeMap.remove(current.localId)
                    } else {
                        showingTimeMap.put(current.localId, true)
                    }
                }

                notifyDataSetChanged()
            }
        }

        open protected fun onContainerLongClick(v: View): Boolean {
            var consumed = false

            withSafeAdapterPosition {
                val current = internalList[it]

                if (!selecting) {
                    selecting = true

                    selectedMap.put(current.localId, true)
                    notifyDataSetChanged()

                    callback?.onMessageSelection(selectedMap.size())

                    consumed = true
                }
            }

            return consumed
        }

        protected open fun applyMessage(message: LocalMessage) {
            text.text = Utils.buildClickableText(text.context, message.message,
                    onWebClickListener = Link.OnClickListener {
                        HttpUrl.parse(it)?.let { callback?.onMessageLinkClick(it) }
                    },
                    onWebLongClickListener = Link.OnLongClickListener {
                        HttpUrl.parse(it)?.let { callback?.onMessageLinkLongClick(it) }
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
            if (selectedMap.get(message.localId, false)) {
                container.cardBackgroundColor = ContextCompat.getColorStateList(container.context, R.color.selected)
            } else {
                container.cardBackgroundColor = ContextCompat.getColorStateList(container.context, R.color.background)
            }
        }

        protected open fun applyTimeVisibility(message: LocalMessage) {
            if (showingTimeMap.get(message.localId, false)) {
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

                if (showingTimeMap.get(current.localId, false)) {
                    time.visibility = View.GONE

                    showingTimeMap.remove(current.localId)
                } else {
                    time.visibility = View.VISIBLE

                    showingTimeMap.put(current.localId, true)
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
        ACTION(7);

        companion object {
            fun from(type: Int) = values().firstOrNull { it.type == type }
                    ?: throw IllegalArgumentException("Unknown type: $type")
        }
    }
}
