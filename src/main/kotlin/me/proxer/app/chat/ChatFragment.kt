package me.proxer.app.chat

import android.arch.lifecycle.Observer
import android.content.ClipData
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import io.reactivex.disposables.Disposable
import kotterknife.bindView
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.chat.sync.ChatNotifications
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Utils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.inputMethodManager
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.toast
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ChatFragment : PagedContentFragment<LocalMessage>() {

    companion object {
        fun newInstance() = ChatFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { ChatViewModelProvider.get(this, conference) }

    override val emptyDataMessage = R.string.error_no_data_chat
    override val isSwipeToRefreshEnabled = false

    override val hostingActivity: ChatActivity
        get() = activity as ChatActivity

    private val chatActivity
        get() = activity as ChatActivity

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Utils.setStatusBarColorIfPossible(activity, R.color.colorPrimary)

            innerAdapter.selectedMessages.let {
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
            IconicsMenuInflaterUtil.inflate(mode.menuInflater, context, R.menu.fragment_chat_cab, menu, true)

            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null

            innerAdapter.clearSelection()

            Utils.setStatusBarColorIfPossible(activity, R.color.colorPrimaryDark)
        }
    }

    private val emojiPopup by lazy {
        val popup = EmojiPopup.Builder.fromRootView(root)
                .setOnEmojiPopupShownListener {
                    emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon.cmd_keyboard))
                }
                .setOnEmojiPopupDismissListener {
                    emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon.cmd_emoticon))
                }
                .build(messageInput)

        popup
    }

    private var actionMode: ActionMode? = null

    private var conference: LocalConference
        get() = chatActivity.conference
        set(value) {
            chatActivity.conference = value
        }

    override val layoutManager by lazy { LinearLayoutManager(context).apply { reverseLayout = true } }
    override var innerAdapter by Delegates.notNull<ChatAdapter>()

    private var pingDisposable: Disposable? = null

    private val emojiButton: ImageButton by bindView(R.id.emojiButton)
    private val inputContainer: ViewGroup by bindView(R.id.inputContainer)
    private val messageInput: EmojiEditText by bindView(R.id.messageInput)
    private val sendButton: FloatingActionButton by bindView(R.id.sendButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = ChatAdapter(savedInstanceState, conference.isGroup)

        innerAdapter.titleClickSubject
                .autoDispose(this)
                .subscribe { ProfileActivity.navigateTo(activity, it.userId, it.username) }

        innerAdapter.messageSelectionSubject
                .autoDispose(this)
                .subscribe {
                    if (it > 0) {
                        when (actionMode) {
                            null -> actionMode = chatActivity.startSupportActionMode(actionModeCallback)
                            else -> actionMode?.invalidate()
                        }

                        actionMode?.title = it.toString()
                    } else {
                        actionMode?.finish()
                    }
                }

        innerAdapter.linkClickSubject
                .autoDispose(this)
                .subscribe { showPage(it) }

        innerAdapter.linkLongClickSubject
                .autoDispose(this)
                .subscribe {
                    getString(R.string.clipboard_title).let { title ->
                        context.clipboardManager.primaryClip = ClipData.newPlainText(title, it.toString())
                        context.toast(R.string.clipboard_status)
                    }
                }

        innerAdapter.mentionsClickSubject
                .autoDispose(this)
                .subscribe { ProfileActivity.navigateTo(activity, username = it) }

        viewModel.conference.observe(this, Observer {
            it?.let { conference = it }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            messageInput.setText(hostingActivity.initialMessage)
        }

        emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon.cmd_emoticon))

        emojiButton.clicks()
                .autoDispose(this)
                .subscribe { emojiPopup.toggle() }

        sendButton.clicks()
                .autoDispose(this)
                .subscribe {
                    messageInput.text.toString().trim().let { text ->
                        if (text.isNotBlank()) {
                            viewModel.sendMessage(text)

                            messageInput.text.clear()

                            scrollToTop()
                        }
                    }
                }
    }

    override fun onResume() {
        super.onResume()

        ChatNotifications.cancel(context)

        pingDisposable = bus.register(ChatFragmentPingEvent::class.java).subscribe()
    }

    override fun onPause() {
        pingDisposable?.dispose()
        pingDisposable = null

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun onDestroyView() {
        emojiPopup.dismiss()

        super.onDestroyView()
    }

    override fun showData(data: List<LocalMessage>) {
        super.showData(data)

        inputContainer.visibility = View.VISIBLE
    }

    override fun hideData() {
        if (innerAdapter.isEmpty()) {
            inputContainer.visibility = View.GONE
        }

        super.hideData()
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        super.showError(action)

        if (innerAdapter.isEmpty()) {
            inputContainer.visibility = View.GONE
        }
    }

    override fun isAtTop() = layoutManager.findFirstVisibleItemPosition() == 0

    private fun generateEmojiDrawable(iconicRes: IIcon) = IconicsDrawable(context)
            .icon(iconicRes)
            .sizeDp(32)
            .paddingDp(6)
            .colorRes(R.color.icon)

    private fun handleCopyClick() {
        val title = getString(R.string.fragment_chat_clip_title)
        val content = innerAdapter.selectedMessages.joinToString(separator = "\n", transform = { it.message })

        context.clipboardManager.primaryClip = ClipData.newPlainText(title, content)
        context.toast(R.string.clipboard_status)

        actionMode?.finish()
    }

    private fun handleReplyClick() {
        messageInput.setText(getString(R.string.fragment_chat_reply, innerAdapter.selectedMessages.first().username))
        messageInput.setSelection(messageInput.text.length)
        messageInput.requestFocus()

        context.inputMethodManager.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT)

        actionMode?.finish()
    }
}
