package me.proxer.app.chat.prv.message

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.view.ActionMode
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxbinding2.support.v7.widget.scrollEvents
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.colorRes
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.isAtTop
import me.proxer.app.util.extension.safeText
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
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

    override val viewModel by viewModel<MessengerViewModel> { parametersOf(conference) }

    override val emptyDataMessage = R.string.error_no_data_chat
    override val isSwipeToRefreshEnabled = false

    override val hostingActivity: MessengerActivity
        get() = activity as MessengerActivity

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Utils.setStatusBarColorIfPossible(activity, R.color.colorPrimary)

            innerAdapter.selectedMessages.let {
                menu.findItem(R.id.reply).isVisible = it.size == 1 && it.first().userId != storageHelper.user?.id
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
                emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon2.cmd_keyboard))
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

        innerAdapter = MessengerAdapter(savedInstanceState, conference.isGroup, storageHelper)

        innerAdapter.titleClickSubject
            .autoDisposable(this.scope())
            .subscribe { ProfileActivity.navigateTo(requireActivity(), it.userId, it.username) }

        innerAdapter.messageSelectionSubject
            .autoDisposable(this.scope())
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
            .autoDisposable(this.scope())
            .subscribe { showPage(it) }

        innerAdapter.linkLongClickSubject
            .autoDisposable(this.scope())
            .subscribe {
                getString(R.string.clipboard_title).let { title ->
                    requireContext().getSystemService<ClipboardManager>()?.primaryClip =
                        ClipData.newPlainText(title, it.toString())

                    requireContext().toast(R.string.clipboard_status)
                }
            }

        innerAdapter.mentionsClickSubject
            .autoDisposable(this.scope())
            .subscribe { ProfileActivity.navigateTo(requireActivity(), username = it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_messenger, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            messageInput.setText(hostingActivity.initialMessage)
        }

        viewModel.conference.observe(viewLifecycleOwner, Observer {
            it?.let { conference = it }
        })

        recyclerView.scrollEvents()
            .observeOn(AndroidSchedulers.mainThread())
            .autoDisposable(viewLifecycleOwner.scope())
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
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                recyclerView.stopScroll()
                layoutManager.scrollToPositionWithOffset(0, 0)
            }

        sendButton.setImageDrawable(
            IconicsDrawable(requireContext(), CommunityMaterial.Icon2.cmd_send)
                .colorRes(requireContext(), R.color.accent)
                .sizeDp(32)
                .paddingDp(4)
        )

        emojiButton.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { emojiPopup.toggle() }

        sendButton.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
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
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { message ->
                viewModel.updateDraft(message.toString())

                messageInput.requestFocus()
            }

        if (savedInstanceState == null) {
            viewModel.loadDraft()
        }

        viewModel.draft.observe(viewLifecycleOwner, Observer {
            if (it != null && messageInput.safeText.isBlank()) messageInput.setText(it)
        })
    }

    override fun onResume() {
        super.onResume()

        MessengerNotifications.cancel(requireContext())

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

        if (wasEmpty && conference.unreadMessageAmount >= 1) {
            recyclerView.scrollToPosition(conference.unreadMessageAmount - 1)
        }

        inputContainer.isVisible = true
    }

    override fun hideData() {
        if (innerAdapter.isEmpty()) {
            inputContainer.isGone = true
        }

        super.hideData()
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        super.showError(action)

        if (innerAdapter.isEmpty()) {
            inputContainer.isGone = true
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

        requireContext().getSystemService<ClipboardManager>()?.primaryClip =
            ClipData.newPlainText(title, content)

        requireContext().toast(R.string.clipboard_status)

        actionMode?.finish()
    }

    private fun handleReplyClick() {
        val username = innerAdapter.selectedMessages.first().username

        messageInput.setText(getString(R.string.fragment_messenger_reply, username))
        messageInput.setSelection(messageInput.safeText.length)
        messageInput.requestFocus()

        requireContext().getSystemService<InputMethodManager>()
            ?.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT)

        actionMode?.finish()
    }
}
