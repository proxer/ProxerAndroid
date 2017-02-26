package com.proxerme.app.fragment.chat

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.proxerme.app.R
import com.proxerme.app.activity.ProfileActivity
import com.proxerme.app.activity.chat.ChatActivity
import com.proxerme.app.adapter.chat.ChatAdapter
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.service.ChatService
import com.proxerme.app.task.ChatTask
import com.proxerme.app.task.ChatTask.ChatInput
import com.proxerme.app.task.RefreshingChatTask
import com.proxerme.app.task.framework.CachedTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.app.util.extension.inputMethodManager
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import okhttp3.HttpUrl
import org.jetbrains.anko.toast

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ChatFragment : PagedLoadingFragment<ChatInput, LocalMessage>() {

    companion object {
        private const val ICON_SIZE = 32
        private const val ICON_PADDING = 6

        fun newInstance(): ChatFragment {
            return ChatFragment()
        }
    }

    override val section = SectionManager.Section.CHAT
    override val itemsOnPage = ChatService.MESSAGES_ON_PAGE
    override val isLoginRequired = true
    override val cacheStrategy = CachedTask.CacheStrategy.EXCEPTION
    override var hasReachedEnd: Boolean
        get() = StorageHelper.hasConferenceReachedEnd(conference.id)
        set(value) {}

    private val chatActivity
        get() = activity as ChatActivity

    override lateinit var layoutManager: LinearLayoutManager
    override lateinit var adapter: ChatAdapter

    private val conference: LocalConference
        get() = chatActivity.conference

    private var actionMode: ActionMode? = null

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Utils.setStatusBarColorIfPossible(activity, R.color.primary)

            menu.findItem(R.id.reply).isVisible = adapter.selectedItems.size == 1 &&
                    adapter.selectedItems.first().userId != StorageHelper.user?.id

            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.copy -> handleCopyMenuItem()
                R.id.reply -> handleReplyMenuItem()
                else -> return false
            }

            return true
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.fragment_chat_cab, menu)

            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null

            adapter.clearSelection()
            Utils.setStatusBarColorIfPossible(activity, R.color.primary_dark)
        }
    }

    private val adapterCallback = object : ChatAdapter.ChatAdapterCallback() {
        override fun onMessageTitleClick(message: LocalMessage) {
            ProfileActivity.navigateTo(activity, message.userId, message.username)
        }

        override fun onMessageSelection(count: Int) {
            if (count > 0) {
                if (actionMode == null) {
                    actionMode = (activity as AppCompatActivity)
                            .startSupportActionMode(actionModeCallback)
                    actionMode?.title = count.toString()
                } else {
                    actionMode?.title = count.toString()
                    actionMode?.invalidate()
                }
            } else {
                actionMode?.finish()
            }
        }

        override fun onMessageLinkClick(link: HttpUrl) {
            showPage(link)
        }

        override fun onMessageLinkLongClick(link: HttpUrl) {
            Utils.setClipboardContent(activity, getString(R.string.fragment_chat_link_clip_title),
                    link.toString())

            context.toast(R.string.clipboard_status)
        }

        override fun onMentionsClick(username: String) {
            ProfileActivity.navigateTo(activity, username = username)
        }
    }

    private lateinit var emojiPopup: EmojiPopup

    private val emojiButton: ImageButton by bindView(R.id.emojiButton)
    private val inputContainer: ViewGroup by bindView(R.id.inputContainer)
    private val messageInput: EmojiEditText by bindView(R.id.messageInput)
    private val sendButton: FloatingActionButton by bindView(R.id.sendButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ChatAdapter(conference.isGroup)
        adapter.user = StorageHelper.user
        adapter.callback = adapterCallback
    }

    override fun onStart() {
        super.onStart()

        ChatService.synchronize(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        layoutManager = LinearLayoutManager(context)

        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutManager.reverseLayout = true

        emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon.cmd_emoticon))
        emojiButton.setOnClickListener {
            emojiPopup.toggle()
        }

        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            val user = StorageHelper.user

            if (!text.isEmpty() && user != null) {
                adapter.insert(arrayOf(context.chatDatabase.insertMessageToSend(user,
                        conference.id, text)))

                ChatService.synchronize(context)

                list.post { list.scrollToPosition(0) }
            }

            messageInput.text.clear()
        }

        emojiPopup = EmojiPopup.Builder.fromRootView(root)
                .setOnEmojiPopupShownListener {
                    emojiButton.setImageDrawable(
                            generateEmojiDrawable(CommunityMaterial.Icon.cmd_keyboard))
                }
                .setOnEmojiPopupDismissListener {
                    emojiButton.setImageDrawable(
                            generateEmojiDrawable(CommunityMaterial.Icon.cmd_emoticon))
                }
                .setOnSoftKeyboardCloseListener { emojiPopup.dismiss() }
                .build(messageInput)

        if (adapter.selectedItems.isNotEmpty()) {
            adapterCallback.onMessageSelection(adapter.selectedItems.size)
        }
    }

    override fun onDestroy() {
        adapter.user = null
        emojiPopup.dismiss()

        super.onDestroy()
    }

    override fun showError(message: Int, buttonMessage: Int,
                           onButtonClickListener: View.OnClickListener?) {
        inputContainer.visibility = View.GONE

        super.showError(message, buttonMessage, onButtonClickListener)
    }

    override fun hideError() {
        inputContainer.visibility = View.VISIBLE

        super.hideError()
    }

    override fun constructTask(): Task<ChatInput, Array<LocalMessage>> {
        return ChatTask(conference.id)
    }

    override fun constructRefreshingTask(): Task<ChatInput, Array<LocalMessage>> {
        return RefreshingChatTask(conference.id, { context })
    }

    override fun constructInput(page: Int): ChatInput {
        return ChatInput(page, context)
    }

    override fun getEmptyMessage(): Int {
        return R.string.error_no_data_chat
    }

    private fun generateEmojiDrawable(iconicRes: IIcon): Drawable {
        return IconicsDrawable(context)
                .icon(iconicRes)
                .sizeDp(ICON_SIZE)
                .paddingDp(ICON_PADDING)
                .colorRes(R.color.icon)
    }

    private fun handleCopyMenuItem() {
        Utils.setClipboardContent(activity, getString(R.string.fragment_chat_clip_title),
                adapter.selectedItems.joinToString(separator = "\n", transform = { it.message }))

        context.toast(R.string.clipboard_status)
        actionMode?.finish()
    }

    private fun handleReplyMenuItem() {
        messageInput.setText(getString(R.string.fragment_chat_reply,
                adapter.selectedItems.first().username))
        messageInput.setSelection(messageInput.text.length)
        messageInput.requestFocus()

        activity.inputMethodManager.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT)
        actionMode?.finish()
    }
}
