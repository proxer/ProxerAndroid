package com.proxerme.app.adapter

import android.os.Bundle
import android.support.v4.util.Pair
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.bindView
import com.bumptech.glide.Glide
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.Utils
import com.proxerme.app.R
import com.proxerme.app.util.TimeUtil
import com.proxerme.library.connection.experimental.chat.entity.Message
import com.proxerme.library.connection.user.entitiy.User
import com.proxerme.library.info.ProxerUrlHolder
import com.stephenvinouze.linkifiedtextview.LinkTextView
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*
import kotlin.comparisons.compareByDescending

/**
 * TODO: Describe class

 * @author Ruben Gees
 */
class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private companion object {
        const val STATE_ITEMS = "adapter_chat_state_items"
        const val STATE_MESSAGE_SELECTED_IDS = "message_selected_ids"
        const val STATE_MESSAGE_SELECTING = "message_selecting"
        const val STATE_MESSAGE_SHOWING_TIME_IDS = "message_showing_time_ids"

        const val TYPE_MESSAGE_INNER = 0
        const val TYPE_MESSAGE_SINGLE = 1
        const val TYPE_MESSAGE_TOP = 2
        const val TYPE_MESSAGE_BOTTOM = 3
        const val TYPE_MESSAGE_SELF_INNER = 4
        const val TYPE_MESSAGE_SELF_SINGLE = 5
        const val TYPE_MESSAGE_SELF_TOP = 6
        const val TYPE_MESSAGE_SELF_BOTTOM = 7
        const val TYPE_ACTION = 8
    }

    var user: User? = null

    private val list: ArrayList<Message>
    private val selectedMap: HashMap<String, Boolean>
    private val showingTimeMap: HashMap<String, Boolean>

    private var selecting = false

    var callback: OnMessageInteractionListener? = null

    constructor(savedInstanceState: Bundle?) {
        setHasStableIds(true)

        this.list = ArrayList<Message>()

        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList(STATE_ITEMS))
        }

        val selectedIds = savedInstanceState?.getStringArrayList(STATE_MESSAGE_SELECTED_IDS)
        val showingTimeIds = savedInstanceState?.getStringArrayList(STATE_MESSAGE_SHOWING_TIME_IDS)

        this.selecting = savedInstanceState?.getBoolean(STATE_MESSAGE_SELECTING) ?: false
        this.selectedMap = HashMap<String, Boolean>()
        this.showingTimeMap = HashMap<String, Boolean>()

        selectedIds?.associateByTo(this.selectedMap, { it }, { true })
        showingTimeIds?.associateByTo(this.showingTimeMap, { it }, { true })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        if (viewType == TYPE_ACTION) {
            return ActionViewHolder(inflater.inflate(R.layout.item_message_action,
                    parent, false))
        } else if (viewType == TYPE_MESSAGE_TOP || viewType == TYPE_MESSAGE_SINGLE) {
            return MessageImageTitleViewHolder(inflater.inflate(R.layout.item_message_single,
                    parent, false))
        } else if (viewType == TYPE_MESSAGE_BOTTOM || viewType == TYPE_MESSAGE_INNER) {
            return MessageViewHolder(inflater.inflate(R.layout.item_message,
                    parent, false))
        } else {
            return MessageViewHolder(inflater.inflate(R.layout.item_message_self,
                    parent, false))
        }
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val margins = getMarginsForPosition(position)

        holder.bind(list[position],
                Utils.convertDpToPx(holder.itemView.context, margins.first.toFloat()),
                Utils.convertDpToPx(holder.itemView.context, margins.second.toFloat()))
    }

    override fun getItemId(position: Int): Long = list[position].id.toLong()

    private fun getMarginsForPosition(position: Int): Pair<Int, Int> {
        val marginTop: Int
        val marginBottom: Int
        val viewType = getItemViewType(position)

        when (viewType) {
            TYPE_MESSAGE_INNER -> {
                marginTop = 2
                marginBottom = 2
            }
            TYPE_MESSAGE_SINGLE -> {
                marginTop = 8
                marginBottom = 8
            }
            TYPE_MESSAGE_TOP -> {
                marginTop = 8
                marginBottom = 2
            }
            TYPE_MESSAGE_BOTTOM -> {
                marginTop = 2
                marginBottom = 8
            }
            TYPE_MESSAGE_SELF_INNER -> {
                marginTop = 2
                marginBottom = 2
            }
            TYPE_MESSAGE_SELF_SINGLE -> {
                marginTop = 8
                marginBottom = 8
            }
            TYPE_MESSAGE_SELF_TOP -> {
                marginTop = 8
                marginBottom = 2
            }
            TYPE_MESSAGE_SELF_BOTTOM -> {
                marginTop = 2
                marginBottom = 8
            }
            TYPE_ACTION -> {
                marginTop = 16
                marginBottom = 16
            }
            else -> throw RuntimeException("An unknown viewType was passed: " + viewType)
        }

        return Pair(marginTop, marginBottom)
    }

    override fun getItemViewType(position: Int): Int {
        var result: Int
        val current = list[position]

        if (!current.action.isBlank()) {
            result = TYPE_ACTION
        } else {
            if (position - 1 < 0) {
                if (position + 1 >= itemCount) {
                    result = TYPE_MESSAGE_SINGLE // The item is the only one
                } else {
                    if (list[position + 1].fromId == current.fromId && list[position + 1].action.isBlank()) {
                        result = TYPE_MESSAGE_BOTTOM // The item is the bottommost item and has an item from the same user above
                    } else {
                        result = TYPE_MESSAGE_SINGLE // The item is the bottommost item and doesn't have an item from the same user above
                    }
                }
            } else if (position + 1 >= itemCount) {
                if (list[position - 1].fromId == current.fromId && list[position - 1].action.isBlank()) {
                    result = TYPE_MESSAGE_TOP // The item is the topmost item and has an item from the same user beneath
                } else {
                    result = TYPE_MESSAGE_SINGLE // The item is the topmost item and doesn't have an item from the same user beneath
                }
            } else {
                if (list[position - 1].fromId == current.fromId && list[position - 1].action.isBlank()) {
                    if (list[position + 1].fromId == current.fromId && list[position + 1].action.isBlank()) {
                        result = TYPE_MESSAGE_INNER // The item is in between two other items from the same user
                    } else {
                        result = TYPE_MESSAGE_TOP // The item has an item from the same user beneath but not above
                    }
                } else {
                    if (list[position + 1].fromId == current.fromId && list[position + 1].action.isBlank()) {
                        result = TYPE_MESSAGE_BOTTOM // The item has an item from the same user above but not beneath
                    } else {
                        result = TYPE_MESSAGE_SINGLE  // The item stands alone
                    }
                }
            }

            if (current.fromId == user?.id) {
                result += 4 // Make the item a "self" item
            }
        }

        return result
    }

    fun addItems(newItems: Collection<Message>) {
        list.addAll(newItems.filter { list.binarySearch(it, compareByDescending { it.time }) < 0 })
        list.sortByDescending { it.time }

        notifyDataSetChanged()
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_ITEMS, list)
        outState.putBoolean(STATE_MESSAGE_SELECTING, selecting)
        outState.putStringArrayList(STATE_MESSAGE_SELECTED_IDS,
                ArrayList(selectedMap.keys))
        outState.putStringArrayList(STATE_MESSAGE_SHOWING_TIME_IDS,
                ArrayList(showingTimeMap.keys))
    }

    fun clear() {
        selecting = false

        list.clear()

        if (selectedMap.size > 0) {
            callback?.onMessageSelection(0)
        }

        selectedMap.clear()
        showingTimeMap.clear()

        notifyDataSetChanged()
    }

    fun clearSelection() {
        selecting = false

        selectedMap.clear()
        notifyDataSetChanged()
    }

    val selectedItems: List<Message>
        get() = list.filter { selectedMap.containsKey(it.id) }.sortedBy { it.time }

    abstract class OnMessageInteractionListener {
        open fun onMessageImageClick(v: View, message: Message) {

        }

        open fun onMessageTitleClick(v: View, message: Message) {

        }

        open fun onMessageSelection(count: Int) {

        }

        open fun onMessageLinkClick(link: String) {

        }
    }

    open inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        protected val container: ViewGroup by bindView(R.id.container)
        protected val text: LinkTextView by bindView(R.id.text)
        protected val time: TextView by bindView(R.id.time)

        init {
            container.setOnClickListener { onContainerClick(it) }
            container.setOnLongClickListener { onContainerLongClick(it) }
            text.setOnLinkClickListener({ view: View, link: String, type: Int ->
                callback?.onMessageLinkClick(link)
            })
        }

        open fun bind(message: Message, marginTop: Int, marginBottom: Int) {
            text.setLinkText(message.message)
            time.text = TimeUtil.convertToRelativeReadableTime(time.context,
                    message.time)

            if (selectedMap.containsKey(message.id)) {
                container.setBackgroundResource(R.color.md_grey_200)
            } else {
                container.setBackgroundResource(android.R.color.white)
            }

            if (showingTimeMap.containsKey(message.id)) {
                time.visibility = View.VISIBLE
            } else {
                time.visibility = View.GONE
            }

            val params = container.layoutParams as ViewGroup.MarginLayoutParams

            params.topMargin = marginTop
            params.bottomMargin = marginBottom

            container.layoutParams = params
        }

        open protected fun onContainerClick(v: View) {
            val current = list[adapterPosition]

            if (selecting) {
                if (selectedMap.containsKey(current.id)) {
                    selectedMap.remove(current.id)

                    if (selectedMap.size <= 0) {
                        selecting = false
                    }
                } else {
                    selectedMap.put(current.id, true)
                }

                callback?.onMessageSelection(selectedMap.size)
            } else {
                if (showingTimeMap.containsKey(current.id)) {
                    showingTimeMap.remove(current.id)
                } else {
                    showingTimeMap.put(current.id, true)
                }
            }

            notifyDataSetChanged()
        }

        open protected fun onContainerLongClick(v: View): Boolean {
            val current = list[adapterPosition]

            if (!selecting) {
                selecting = true

                selectedMap.put(current.id, true)
                notifyDataSetChanged()

                callback?.onMessageSelection(selectedMap.size)

                return true
            }

            return false
        }
    }

    inner class MessageImageTitleViewHolder(itemView: View) : MessageViewHolder(itemView) {

        private val image: CircleImageView by bindView(R.id.image)
        private val title: TextView by bindView(R.id.title)

        init {
            image.setOnClickListener {
                callback?.onMessageImageClick(it, list[adapterPosition])
            }

            title.setOnClickListener {
                callback?.onMessageTitleClick(it, list[adapterPosition])
            }
        }

        override fun bind(message: Message, marginTop: Int, marginBottom: Int) {
            super.bind(message, marginTop, marginBottom)

            title.text = message.username

            if (message.imageId.isBlank()) {
                image.setImageDrawable(IconicsDrawable(image.context)
                        .icon(CommunityMaterial.Icon.cmd_account)
                        .sizeDp(32)
                        .paddingDp(4)
                        .colorRes(R.color.colorPrimary))
            } else {
                Glide.with(image.context)
                        .load(ProxerUrlHolder.getUserImageUrl(message.imageId))
                        .into(image)
            }
        }
    }

    inner class ActionViewHolder(itemView: View) : MessageViewHolder(itemView) {

        override fun onContainerClick(v: View) {
            val current = list[adapterPosition]

            if (showingTimeMap.containsKey(current.id)) {
                time.visibility = View.GONE

                showingTimeMap.remove(current.id)
            } else {
                time.visibility = View.VISIBLE

                showingTimeMap.put(current.id, true)
            }
        }

        override fun onContainerLongClick(v: View): Boolean {
            return false
        }
    }
}
