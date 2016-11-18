package com.proxerme.app.adapter.chat

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.util.Pair
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.bindView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.util.ParcelableLongSparseArray
import com.proxerme.app.util.TimeUtil
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.user.entitiy.User
import com.proxerme.library.parameters.ActionParameter

/**
 * TODO: Describe class

 * @author Ruben Gees
 */
class ChatAdapter(savedInstanceState: Bundle? = null, val isGroup: Boolean) :
        PagingAdapter<LocalMessage>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_chat_state_items"
        private const val MESSAGE_SELECTED_IDS_STATE = "adapter_chat_state_message_selected_ids"
        private const val MESSAGE_SELECTING_STATE = "adapter_chat_state_message_selecting"
        private const val MESSAGE_SHOWING_TIME_IDS_STATE = "adapter_chat_state_showing_time_ids"

        private const val TYPE_MESSAGE_INNER = 0
        private const val TYPE_MESSAGE_SINGLE = 1
        private const val TYPE_MESSAGE_TOP = 2
        private const val TYPE_MESSAGE_BOTTOM = 3
        private const val TYPE_MESSAGE_SELF_INNER = 4
        private const val TYPE_MESSAGE_SELF_SINGLE = 5
        private const val TYPE_MESSAGE_SELF_TOP = 6
        private const val TYPE_MESSAGE_SELF_BOTTOM = 7
        private const val TYPE_ACTION = 8
    }

    var user: User? = null

    private val selectedMap: ParcelableLongSparseArray
    private val showingTimeMap: ParcelableLongSparseArray

    var callback: ChatAdapterCallback? = null

    private var selecting: Boolean

    val selectedItems: List<LocalMessage>
        get() = list.filter { selectedMap.get(it.localId, false) }.sortedBy { it.time }

    init {
        if (savedInstanceState == null) {
            selectedMap = ParcelableLongSparseArray()
            showingTimeMap = ParcelableLongSparseArray()
            selecting = false
        } else {
            selectedMap = savedInstanceState.getParcelable(MESSAGE_SELECTED_IDS_STATE)
            showingTimeMap = savedInstanceState.getParcelable(MESSAGE_SHOWING_TIME_IDS_STATE)
            selecting = savedInstanceState.getBoolean(MESSAGE_SELECTING_STATE)
            list.addAll(savedInstanceState.getParcelableArrayList(ITEMS_STATE))
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        if (viewType == TYPE_ACTION) {
            return ActionViewHolder(inflater.inflate(R.layout.item_message_action,
                    parent, false))
        } else if (viewType == TYPE_MESSAGE_TOP || viewType == TYPE_MESSAGE_SINGLE) {
            if (isGroup) {
                return MessageTitleViewHolder(inflater.inflate(R.layout.item_message_single,
                        parent, false))
            } else {
                return MessageViewHolder(inflater.inflate(R.layout.item_message,
                        parent, false))
            }
        } else if (viewType == TYPE_MESSAGE_BOTTOM || viewType == TYPE_MESSAGE_INNER) {
            return MessageViewHolder(inflater.inflate(R.layout.item_message,
                    parent, false))
        } else {
            return MessageViewHolder(inflater.inflate(R.layout.item_message_self,
                    parent, false))
        }
    }

    override fun onBindViewHolder(holder: PagingViewHolder<LocalMessage>,
                                  position: Int) {
        holder as MessageViewHolder

        val margins = getMarginsForPosition(position)

        holder.bind(list[position],
                Utils.convertDpToPx(holder.itemView.context, margins.first.toFloat()),
                Utils.convertDpToPx(holder.itemView.context, margins.second.toFloat()))
    }

    override fun getItemId(position: Int): Long = list[position].localId

    private fun getMarginsForPosition(position: Int): Pair<Int, Int> {
        val marginTop: Int
        val marginBottom: Int
        val viewType = getItemViewType(position)

        when (viewType) {
            TYPE_MESSAGE_INNER -> {
                marginTop = 0
                marginBottom = 0
            }
            TYPE_MESSAGE_SINGLE -> {
                marginTop = 6
                marginBottom = 6
            }
            TYPE_MESSAGE_TOP -> {
                marginTop = 6
                marginBottom = 0
            }
            TYPE_MESSAGE_BOTTOM -> {
                marginTop = 0
                marginBottom = 6
            }
            TYPE_MESSAGE_SELF_INNER -> {
                marginTop = 0
                marginBottom = 0
            }
            TYPE_MESSAGE_SELF_SINGLE -> {
                marginTop = 6
                marginBottom = 6
            }
            TYPE_MESSAGE_SELF_TOP -> {
                marginTop = 6
                marginBottom = 0
            }
            TYPE_MESSAGE_SELF_BOTTOM -> {
                marginTop = 0
                marginBottom = 6
            }
            TYPE_ACTION -> {
                marginTop = 12
                marginBottom = 12
            }
            else -> throw RuntimeException("An unknown viewType was passed: " + viewType)
        }

        return Pair(marginTop, marginBottom)
    }

    override fun getItemViewType(position: Int): Int {
        var result: Int
        val current = list[position]

        if (current.action != ActionParameter.NONE) {
            result = TYPE_ACTION
        } else {
            if (position - 1 < 0) {
                if (position + 1 >= itemCount) {
                    result = TYPE_MESSAGE_SINGLE // The item is the only one
                } else {
                    if (list[position + 1].userId == current.userId &&
                            list[position + 1].action == ActionParameter.NONE) {
                        result = TYPE_MESSAGE_BOTTOM // The item is the bottommost item and has an item from the same user above
                    } else {
                        result = TYPE_MESSAGE_SINGLE // The item is the bottommost item and doesn't have an item from the same user above
                    }
                }
            } else if (position + 1 >= itemCount) {
                if (list[position - 1].userId == current.userId &&
                        list[position - 1].action == ActionParameter.NONE) {
                    result = TYPE_MESSAGE_TOP // The item is the topmost item and has an item from the same user beneath
                } else {
                    result = TYPE_MESSAGE_SINGLE // The item is the topmost item and doesn't have an item from the same user beneath
                }
            } else {
                if (list[position - 1].userId == current.userId &&
                        list[position - 1].action == ActionParameter.NONE) {
                    if (list[position + 1].userId == current.userId &&
                            list[position + 1].action == ActionParameter.NONE) {
                        result = TYPE_MESSAGE_INNER // The item is in between two other items from the same user
                    } else {
                        result = TYPE_MESSAGE_TOP // The item has an item from the same user beneath but not above
                    }
                } else {
                    if (list[position + 1].userId == current.userId &&
                            list[position + 1].action == ActionParameter.NONE) {
                        result = TYPE_MESSAGE_BOTTOM // The item has an item from the same user above but not beneath
                    } else {
                        result = TYPE_MESSAGE_SINGLE  // The item stands alone
                    }
                }
            }

            if (current.userId == user?.id) {
                result += 4 // Make the item a "self" item
            }
        }

        return result
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ITEMS_STATE, list)
        outState.putBoolean(MESSAGE_SELECTING_STATE, selecting)
        outState.putParcelable(MESSAGE_SELECTED_IDS_STATE, selectedMap)
        outState.putParcelable(MESSAGE_SHOWING_TIME_IDS_STATE, showingTimeMap)
    }

    override fun clear() {
        list.clear()
        showingTimeMap.clear()

        clearSelection()

        if (selectedMap.size() > 0) {
            callback?.onMessageSelection(0)
        }

        notifyDataSetChanged()
    }

    override fun removeCallback() {
        callback = null
    }

    fun clearSelection() {
        selecting = false

        selectedMap.clear()
        notifyDataSetChanged()
    }

    abstract class ChatAdapterCallback {
        open fun onMessageTitleClick(message: LocalMessage) {

        }

        open fun onMessageSelection(count: Int) {

        }

        open fun onMessageLinkClick(link: String) {

        }

        open fun onMessageLinkLongClick(link: String) {

        }

        open fun onMentionsClick(username: String) {

        }
    }

    open inner class MessageViewHolder(itemView: View) :
            PagingAdapter.PagingViewHolder<LocalMessage>(itemView) {

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
            if (adapterPosition != RecyclerView.NO_POSITION) {
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
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val current = list[adapterPosition]

                if (!selecting) {
                    selecting = true

                    selectedMap.put(current.localId, true)
                    notifyDataSetChanged()

                    callback?.onMessageSelection(selectedMap.size())

                    return true
                }
            }

            return false
        }

        protected open fun applyMessage(message: LocalMessage) {
            text.text = Utils.buildClickableText(text.context, message.message,
                    onWebClickListener = Link.OnClickListener {
                        callback?.onMessageLinkClick(it)
                    },
                    onWebLongClickListener = Link.OnLongClickListener {
                        callback?.onMessageLinkLongClick(it)
                    },
                    onMentionsClickListener = Link.OnClickListener {
                        callback?.onMentionsClick(it.trim().substring(1))
                    })
        }

        protected open fun applyTime(message: LocalMessage) {
            time.text = TimeUtil.convertToRelativeReadableTime(time.context,
                    message.time)
        }

        protected open fun applySendStatus(message: LocalMessage) {
            if (message.id.toLong() < 0) {
                text.setCompoundDrawables(null, null,
                        IconicsDrawable(text.context)
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
                container.cardBackgroundColor = ContextCompat
                        .getColorStateList(container.context, R.color.selected_card)
            } else {
                container.cardBackgroundColor = ContextCompat
                        .getColorStateList(container.context, R.color.cardview_background)
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

    inner class MessageTitleViewHolder(itemView: View) : MessageViewHolder(itemView) {

        private val title: TextView by bindView(R.id.title)

        init {
            title.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onMessageTitleClick(list[adapterPosition])
                }
            }
        }

        override fun bind(message: LocalMessage, marginTop: Int, marginBottom: Int) {
            super.bind(message, marginTop, marginBottom)

            title.text = message.username
        }
    }

    inner class ActionViewHolder(itemView: View) : MessageViewHolder(itemView) {

        override fun onContainerClick(v: View) {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val current = list[adapterPosition]

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
                ActionParameter.ADD_USER -> text.context.getString(R.string.action_add_user,
                        "@${message.username}", "@${message.message}")
                ActionParameter.REMOVE_USER -> text.context.getString(R.string.action_remove_user,
                        "@${message.username}", "@${message.message}")
                ActionParameter.SET_LEADER -> text.context.getString(R.string.action_set_leader,
                        "@${message.username}", "@${message.message}")
                ActionParameter.SET_TOPIC -> text.context.getString(R.string.action_set_topic,
                        "@${message.username}", message.message)
                else -> message.message
            }
        }
    }
}
