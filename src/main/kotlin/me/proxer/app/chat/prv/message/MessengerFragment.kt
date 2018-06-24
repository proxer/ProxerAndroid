package me.proxer.app.chat.prv.message

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
import android.widget.ImageView
import com.jakewharton.rxbinding2.support.v7.widget.scrollEvents
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotterknife.bindView
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.messengerDao
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Utils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.colorRes
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.inputMethodManager
import me.proxer.app.util.extension.isAtTop
import me.proxer.app.util.extension.safeText
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.toast
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MessengerFragment : PagedContentFragment<LocalMessage>() {

    companion object {
        fun newInstance() = MessengerFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { MessengerViewModelProvider.get(this, conference) }

    override val emptyDataMessage = R.string.error_no_data_chat
    override val isSwipeToRefreshEnabled = false

    override val hostingActivity: MessengerActivity
        get() = activity as MessengerActivity

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
            IconicsMenuInflaterUtil.inflate(mode.menuInflater, context, R.menu.fragment_messenger_cab, menu, true)

            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null

            innerAdapter.clearSelection()
            innerAdapter.notifyDataSetChanged()

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
        get() = hostingActivity.conference
        set(value) {
            hostingActivity.conference = value
        }

    override val layoutManager by lazy { LinearLayoutManager(context).apply { reverseLayout = true } }
    override var innerAdapter by Delegates.notNull<MessengerAdapter>()

    private var pingDisposable: Disposable? = null

    private val scrollToBottom: FloatingActionButton by bindView(R.id.scrollToBottom)
    private val emojiButton: ImageButton by bindView(R.id.emojiButton)
    private val inputContainer: ViewGroup by bindView(R.id.inputContainer)
    private val messageInput: EmojiEditText by bindView(R.id.messageInput)
    private val sendButton: ImageView by bindView(R.id.sendButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = MessengerAdapter(savedInstanceState, conference.isGroup)

        innerAdapter.titleClickSubject
            .autoDispose(this)
            .subscribe { ProfileActivity.navigateTo(requireActivity(), it.userId, it.username) }

        innerAdapter.messageSelectionSubject
            .autoDispose(this)
            .subscribe {
                if (it > 0) {
                    when (actionMode) {
                        null -> actionMode = hostingActivity.startSupportActionMode(actionModeCallback)
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
                    requireContext().clipboardManager.primaryClip = ClipData.newPlainText(title, it.toString())
                    requireContext().toast(R.string.clipboard_status)
                }
            }

        innerAdapter.mentionsClickSubject
            .autoDispose(this)
            .subscribe { ProfileActivity.navigateTo(requireActivity(), username = it) }

        viewModel.conference.observe(this, Observer {
            it?.let { conference = it }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_messenger, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            messageInput.setText(hostingActivity.initialMessage)
        }

        recyclerView.scrollEvents()
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this)
            .subscribe {
                val currentPosition = layoutManager.findFirstVisibleItemPosition()

                when (currentPosition <= innerAdapter.enqueuedMessageCount) {
                    true -> scrollToBottom.hide()
                    false -> scrollToBottom.show()
                }
            }

        emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon.cmd_emoticon))

        scrollToBottom.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32, colorRes = R.color.textColorPrimary)

        scrollToBottom.clicks()
            .autoDispose(this)
            .subscribe {
                recyclerView.stopScroll()
                layoutManager.scrollToPositionWithOffset(0, 0)
            }

        sendButton.setImageDrawable(IconicsDrawable(requireContext(), CommunityMaterial.Icon.cmd_send)
            .colorRes(requireContext(), R.color.accent)
            .sizeDp(32)
            .paddingDp(4))

        emojiButton.clicks()
            .autoDispose(this)
            .subscribe { emojiPopup.toggle() }

        sendButton.clicks()
            .autoDispose(this)
            .subscribe {
                messageInput.text.toString().trim().let { text ->
                    if (text.isNotBlank()) {
                        viewModel.sendMessage(text)

                        messageInput.safeText.clear()

                        scrollToTop()
                    }
                }
            }

        messageInput.textChanges()
            .skipInitialValue()
            .autoDispose(this)
            .subscribe { message ->
                viewModel.updateDraft(message.toString())

                messageInput.requestFocus()
            }

        viewModel.draft.observe(this, Observer {
            if (it != null && messageInput.safeText.isBlank()) messageInput.setText(it)
        })

        if (savedInstanceState == null) {
            viewModel.loadDraft()
        }
    }

    override fun onResume() {
        super.onResume()

        pingDisposable = bus.register(MessengerFragmentPingEvent::class.java).subscribe()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun onPause() {
        pingDisposable?.dispose()
        pingDisposable = null

        super.onPause()
    }

    override fun onDestroyView() {
        emojiPopup.dismiss()

        super.onDestroyView()
    }

    override fun showData(data: List<LocalMessage>) {
        val wasEmpty = innerAdapter.isEmpty()

        super.showData(data)

        if (wasEmpty) {
            Completable
                .fromCallable {
                    val unreadMap = messengerDao.getUnreadConferences().associate {
                        it to messengerDao.getMostRecentMessagesForConference(it.id, it.unreadMessageAmount)
                            .asReversed()
                    }

                    context?.let { context -> MessengerNotifications.showOrUpdate(context, unreadMap) }
                }
                .subscribeOn(Schedulers.io())
                .subscribeAndLogErrors()

            if (conference.unreadMessageAmount >= 1) {
                recyclerView.scrollToPosition(conference.unreadMessageAmount - 1)
            }
        }

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

    override fun isAtTop() = layoutManager.isAtTop()

    private fun generateEmojiDrawable(iconicRes: IIcon) = IconicsDrawable(context)
        .icon(iconicRes)
        .sizeDp(32)
        .paddingDp(6)
        .iconColor(requireContext())

    private fun handleCopyClick() {
        val title = getString(R.string.fragment_messenger_clip_title)
        val content = innerAdapter.selectedMessages.joinToString(separator = "\n", transform = { it.message })

        requireContext().clipboardManager.primaryClip = ClipData.newPlainText(title, content)
        requireContext().toast(R.string.clipboard_status)

        actionMode?.finish()
    }

    private fun handleReplyClick() {
        val username = innerAdapter.selectedMessages.first().username

        messageInput.setText(getString(R.string.fragment_messenger_reply, username))
        messageInput.setSelection(messageInput.safeText.length)
        messageInput.requestFocus()

        requireContext().inputMethodManager.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT)

        actionMode?.finish()
    }
}
