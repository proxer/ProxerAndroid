package me.proxer.app.fragment.chat

import android.content.ClipData
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.rubengees.ktask.util.TaskBuilder
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import me.proxer.app.R
import me.proxer.app.activity.ChatActivity
import me.proxer.app.activity.ProfileActivity
import me.proxer.app.adapter.chat.ChatAdapter
import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.entity.chat.LocalMessage
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.helper.StorageHelper
import me.proxer.app.job.ChatJob
import me.proxer.app.task.chat.ChatRefreshTask
import me.proxer.app.task.chat.ChatTask
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.inputMethodManager
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast

/**
 * @author Ruben Gees
 */
class ChatFragment : PagedLoadingFragment<Int, LocalMessage>() {

    companion object {
        var isActive: Boolean = false
            private set

        fun newInstance(): ChatFragment {
            return ChatFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val itemsOnPage = ChatJob.MESSAGES_ON_PAGE
    override val isLoginRequired = true
    override val emptyResultMessage = R.string.error_no_data_chat
    override var hasReachedEnd: Boolean
        get() = conference.isLoadedFully
        set(value) {}

    private val chatActivity
        get() = activity as ChatActivity

    override val layoutManager by lazy { LinearLayoutManager(context).apply { reverseLayout = true } }
    override lateinit var innerAdapter: ChatAdapter

    private var conference: LocalConference
        get() = chatActivity.conference
        set(value) {
            chatActivity.conference = value
        }

    private var actionMode: ActionMode? = null

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Utils.setStatusBarColorIfPossible(activity, R.color.colorPrimary)

            innerAdapter.selectedItems.let {
                menu.findItem(R.id.reply).isVisible = it.size == 1 && it.first().userId != StorageHelper.user?.id
            }

            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.copy -> handleCopyClick()
                R.id.reply -> handleReplyClick()
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

            innerAdapter.clearSelection()

            Utils.setStatusBarColorIfPossible(activity, R.color.colorPrimaryDark)
        }
    }

    private val adapterCallback = object : ChatAdapter.ChatAdapterCallback {
        override fun onMessageTitleClick(message: LocalMessage) {
            ProfileActivity.navigateTo(activity, message.userId, message.username)
        }

        override fun onMessageSelection(count: Int) {
            if (count > 0) {
                if (actionMode == null) {
                    actionMode = chatActivity.startSupportActionMode(actionModeCallback)
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
            val title = getString(R.string.clipboard_title)

            context.clipboardManager.primaryClip = ClipData.newPlainText(title, link.toString())
            context.toast(R.string.clipboard_status)
        }

        override fun onMentionsClick(username: String) {
            ProfileActivity.navigateTo(activity, username = username)
        }
    }

    private val emojiPopup by lazy {
        EmojiPopup.Builder.fromRootView(root)
                .setOnEmojiPopupShownListener {
                    emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon.cmd_keyboard))
                }
                .setOnEmojiPopupDismissListener {
                    emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon.cmd_emoticon))
                }
                .build(messageInput)
    }

    private val emojiButton: ImageButton by bindView(R.id.emojiButton)
    private val inputContainer: ViewGroup by bindView(R.id.inputContainer)
    private val messageInput: EmojiEditText by bindView(R.id.messageInput)
    private val sendButton: FloatingActionButton by bindView(R.id.sendButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = ChatAdapter(conference.isGroup)

        innerAdapter.user = StorageHelper.user
        innerAdapter.callback = adapterCallback

        // Does not actually do anything, just registers the EventBus.
        refreshTask.forceExecute(0)
    }

    override fun onResume() {
        super.onResume()

        isActive = true

        if (!ChatJob.isRunning()) {
            ChatJob.scheduleSynchronization()
        }
    }

    override fun onPause() {
        isActive = false

        super.onPause()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon.cmd_emoticon))
        emojiButton.setOnClickListener {
            emojiPopup.toggle()
        }

        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            val user = StorageHelper.user

            if (!text.isEmpty() && user != null) {
                doAsync {
                    innerAdapter.insert(listOf(chatDb.insertMessageToSend(user, conference.id, text)))

                    ChatJob.scheduleSynchronization()

                    list.post { list.scrollToPosition(0) }
                }
            }

            messageInput.text.clear()
        }

        innerAdapter.selectedItems.let {
            if (it.isNotEmpty()) {
                adapterCallback.onMessageSelection(it.size)
            }
        }
    }

    override fun showError(message: Int, buttonMessage: Int, buttonAction: View.OnClickListener?) {
        inputContainer.visibility = View.GONE

        super.showError(message, buttonMessage, buttonAction)
    }

    override fun hideError() {
        inputContainer.visibility = View.VISIBLE

        super.hideError()
    }

    override fun constructTask() = TaskBuilder.task(ChatTask(conference.id))
            .async()
            .map {
                conference = it.conference

                it.messages
            }
            .build()

    override fun constructRefreshTask() = TaskBuilder.task(ChatRefreshTask(conference.id))
            .map {
                conference = it.conference

                it.messages
            }
            .build()

    override fun constructPagedInput(page: Int) = page

    private fun generateEmojiDrawable(iconicRes: IIcon): Drawable {
        return IconicsDrawable(context)
                .icon(iconicRes)
                .sizeDp(32)
                .paddingDp(6)
                .colorRes(R.color.icon)
    }

    private fun handleCopyClick() {
        val title = getString(R.string.fragment_chat_clip_title)
        val content = innerAdapter.selectedItems.joinToString(separator = "\n", transform = { it.message })

        context.clipboardManager.primaryClip = ClipData.newPlainText(title, content)
        context.toast(R.string.clipboard_status)

        actionMode?.finish()
    }

    private fun handleReplyClick() {
        messageInput.setText(getString(R.string.fragment_chat_reply, innerAdapter.selectedItems.first().username))
        messageInput.setSelection(messageInput.text.length)
        messageInput.requestFocus()

        activity.inputMethodManager.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT)

        actionMode?.finish()
    }
}
