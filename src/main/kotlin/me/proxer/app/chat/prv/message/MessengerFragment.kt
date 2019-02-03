package me.proxer.app.chat.prv.message

import android.annotation.SuppressLint
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
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import io.reactivex.android.schedulers.AndroidSchedulers
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.colorAttr
import me.proxer.app.util.extension.isAtTop
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.safeText
import me.proxer.app.util.extension.scrollToTop
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.toast
import me.proxer.app.util.extension.unsafeLazy
import org.koin.androidx.viewmodel.ext.viewModel
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
            Utils.setStatusBarColorIfPossible(activity, requireContext().resolveColor(R.attr.colorPrimary))

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

            Utils.setStatusBarColorIfPossible(activity, requireContext().resolveColor(R.attr.colorPrimaryDark))
        }
    }

    private val emojiPopup by unsafeLazy {
        val popup = EmojiPopup.Builder.fromRootView(root)
            .setOnEmojiPopupShownListener { updateInput() }
            .setOnEmojiPopupDismissListener { updateInput() }
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

    private val scrollToBottom: FloatingActionButton by bindView(R.id.scrollToBottom)
    private val emojiButton: ImageButton by bindView(R.id.emojiButton)
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

        // Call getter as soon as possible to make keyboard detection work properly.
        emojiPopup
        updateInput()

        if (savedInstanceState == null) {
            messageInput.setText(hostingActivity.initialMessage)
        }

        viewModel.conference.observe(viewLifecycleOwner, Observer {
            it?.let { conference = it }
        })

        scrollToBottom.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32, colorAttr = R.attr.colorOnSurface)

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

        scrollToBottom.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                recyclerView.stopScroll()
                layoutManager.scrollToPositionWithOffset(0, 0)
            }

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

                        // Scroll to top without animation. If at top, a smooth scroll is started.
                        if (!isAtTop()) {
                            recyclerView.scrollToTop()
                        }
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

        bus.register(MessengerFragmentPingEvent::class.java)
            .autoDisposable(this.scope())
            .subscribe()
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
        val wasEmpty = innerAdapter.isEmpty()

        super.showData(data)

        if (wasEmpty && conference.unreadMessageAmount >= 1) {
            recyclerView.scrollToPosition(conference.unreadMessageAmount - 1)
        }

        updateInput()
    }

    override fun hideData() {
        updateInput()

        super.hideData()
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        super.showError(action)

        updateInput()
    }

    override fun isAtTop() = recyclerView.isAtTop()

    private fun updateInput() {
        val isLoggedIn by unsafeLazy { storageHelper.isLoggedIn }
        val shouldEnabledInput = innerAdapter.isEmpty().not() && isLoggedIn

        if (shouldEnabledInput) {
            emojiButton.isEnabled = true
            sendButton.isEnabled = true
            messageInput.isEnabled = true

            messageInput.hint = getString(R.string.fragment_messenger_message)
        } else {
            emojiButton.isEnabled = false
            sendButton.isEnabled = false
            messageInput.isEnabled = false

            messageInput.hint = when {
                innerAdapter.isEmpty() -> getString(R.string.fragment_chat_loading_message)
                else -> getString(R.string.fragment_chat_login_required_message)
            }
        }

        updateIcons(!shouldEnabledInput)
    }

    @SuppressLint("PrivateResource")
    private fun updateIcons(disabledColor: Boolean) {
        val emojiButtonIcon: IIcon = when (emojiPopup.isShowing) {
            true -> CommunityMaterial.Icon2.cmd_keyboard
            false -> CommunityMaterial.Icon.cmd_emoticon
        }

        emojiButton.setImageDrawable(
            IconicsDrawable(requireContext(), emojiButtonIcon)
                .colorAttr(requireContext(), when (disabledColor) {
                    true -> R.attr.colorOnSurfaceDisabled
                    false -> R.attr.colorIcon
                })
                .sizeDp(32)
                .paddingDp(6)
        )

        sendButton.setImageDrawable(
            IconicsDrawable(requireContext(), CommunityMaterial.Icon2.cmd_send)
                .colorAttr(requireContext(), when (disabledColor) {
                    true -> R.attr.colorOnSurfaceDisabled
                    false -> R.attr.colorSecondary
                })
                .sizeDp(32)
                .paddingDp(4)
        )
    }

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
