package com.proxerme.app.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.SQLException
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import butterknife.bindView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.ChatAdapter
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.event.ChatEvent
import com.proxerme.app.fragment.framework.MainFragment
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.interfaces.OnActivityListener
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.manager.UserManager
import com.proxerme.app.module.LoginModule
import com.proxerme.app.service.ChatService
import com.proxerme.app.util.Utils
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.inputMethodManager
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ChatFragment : MainFragment(), OnActivityListener {

    companion object {
        private const val ARGUMENT_CONFERENCE = "conference"

        fun newInstance(conference: LocalConference): ChatFragment {

            return ChatFragment().apply {
                this.arguments = Bundle().apply {
                    this.putParcelable(ARGUMENT_CONFERENCE, conference)
                }
            }
        }
    }

    private lateinit var conference: LocalConference

    override val section: SectionManager.Section = SectionManager.Section.CHAT

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: ChatAdapter

    private var refreshTask: Future<Unit>? = null

    private val loginModule = LoginModule(object : LoginModule.LoginModuleCallback {
        override val activity: AppCompatActivity
            get() = this@ChatFragment.activity as AppCompatActivity

        override fun showError(message: String, buttonMessage: String?,
                               onButtonClickListener: View.OnClickListener?) {
            this@ChatFragment.showError(message, buttonMessage, onButtonClickListener)
        }

        override fun load(showProgress: Boolean) {
            refresh()
        }
    })

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Utils.setStatusBarColorIfPossible(activity, R.color.primary)

            if (adapter.selectedItems.size == 1 &&
                    adapter.selectedItems.first().userId != StorageHelper.user?.id ?: null) {
                menu.findItem(R.id.reply).isVisible = true
            } else {
                menu.findItem(R.id.reply).isVisible = false
            }

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

    private var actionMode: ActionMode? = null

    private val adapterCallback = object : ChatAdapter.OnMessageInteractionListener() {
        override fun onMessageTitleClick(v: View, message: LocalMessage) {
            UserActivity.navigateTo(activity, message.userId, message.username)
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

        override fun onMessageLinkClick(link: String) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            } catch (exception: ActivityNotFoundException) {
                context.toast(R.string.link_error_not_found)
            }
        }

        override fun onMessageLinkLongClick(link: String) {
            Utils.setClipboardContent(activity, getString(R.string.fragment_chat_link_clip_title),
                    link)

            context.toast(R.string.fragment_chat_clip_status)
        }

        override fun onMentionsClick(username: String) {
            UserActivity.navigateTo(activity, username = username)
        }
    }

    private val contentRoot: ViewGroup by bindView(R.id.contentRoot)
    private val messageInput: EditText by bindView(R.id.messageInput)
    private val sendButton: FloatingActionButton by bindView(R.id.sendButton)
    private val list: RecyclerView by bindView(R.id.list)
    private val progress: SwipeRefreshLayout by bindView(R.id.progress)
    private val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    private val errorText: TextView by bindView(R.id.errorText)
    private val errorButton: Button by bindView(R.id.errorButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.cancelNotification(context, NotificationHelper.CHAT_NOTIFICATION)

        conference = arguments.getParcelable(ARGUMENT_CONFERENCE)

        adapter = ChatAdapter(savedInstanceState, conference.isGroup)
        adapter.user = UserManager.user
        adapter.callback = adapterCallback

        layoutManager = LinearLayoutManager(context)
        layoutManager.reverseLayout = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = adapter
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (!StorageHelper.hasConferenceReachedEnd(conference.id) &&
                        !ChatService.isLoadingMessages(conference.id) &&
                        refreshTask?.isDone ?: true) {
                    ChatService.loadMoreMessages(context, conference.id)
                }
            }
        })

        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()

            if (!text.isEmpty()) {
                context.chatDatabase.insertMessageToSend(StorageHelper.user!!, conference.id, text)

                if (loginModule.canLoad()) {
                    refresh()

                    if (!ChatService.isSynchronizing) {
                        ChatService.synchronize(context)
                    }
                }
            }

            messageInput.text.clear()
        }

        errorText.movementMethod = TouchableMovementMethod.getInstance()
        progress.setColorSchemeColors(ContextCompat.getColor(context,
                R.color.primary))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (adapter.selectedItems.size > 0) {
            adapterCallback.onMessageSelection(adapter.selectedItems.size)
        }
    }

    override fun onResume() {
        super.onResume()

        loginModule.onResume()

        if (loginModule.canLoad()) {
            if (!ChatService.isSynchronizing) {
                ChatService.synchronize(context)
            }

            refresh()
        }
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
        loginModule.onStart()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        loginModule.onStop()

        super.onStop()
    }

    override fun onDestroy() {
        refreshTask?.cancel(true)

        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    @Subscribe
    fun onMessagesChanged(@Suppress("UNUSED_PARAMETER") event: ChatEvent) {
        if (loginModule.canLoad()) {
            refresh()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoadMoreMessagesFailed(exception: ChatService.LoadMoreMessagesException) {
        if (!StorageHelper.hasConferenceReachedEnd(conference.id) &&
                exception.conferenceId == conference.id) {
            showError(exception.message!!)
        }
    }

    private fun showError(message: String, buttonMessage: String? = null,
                          onButtonClickListener: View.OnClickListener? = null) {
        clear()

        contentRoot.visibility = View.INVISIBLE
        errorContainer.visibility = View.VISIBLE
        errorText.text = Utils.buildClickableText(context, message,
                onWebClickListener = Link.OnClickListener { link ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse(link + "?device=mobile")))
                    } catch (exception: ActivityNotFoundException) {
                        context.toast(R.string.link_error_not_found)
                    }
                })

        if (buttonMessage == null) {
            errorButton.text = getString(R.string.error_retry)
        } else {
            errorButton.text = buttonMessage
        }

        if (onButtonClickListener == null) {
            errorButton.setOnClickListener {
                if (loginModule.canLoad()) {
                    refresh()
                }
            }
        } else {
            errorButton.setOnClickListener(onButtonClickListener)
        }
    }

    private fun hideError() {
        errorContainer.visibility = View.INVISIBLE
        contentRoot.visibility = View.VISIBLE
        adapter.user = UserManager.user
    }

    private fun clear() {
        adapter.clear()
        adapter.user = null
    }

    private fun refresh() {
        refreshTask?.cancel(true)

        hideError()

        refreshTask = doAsync {
            try {
                val messages = context.chatDatabase.getMessages(conference.id)

                if (messages.isEmpty()) {
                    if (!StorageHelper.hasConferenceReachedEnd(conference.id) &&
                            !ChatService.isLoadingMessages(conference.id)) {
                        showProgress()

                        ChatService.loadMoreMessages(context, conference.id)
                    } else {
                        hideProgress()

                        //TODO show empty view
                    }
                } else {
                    uiThread {
                        hideError()
                        hideProgress()

                        adapter.replace(messages)
                    }
                }
            } catch(exception: SQLException) {
                uiThread {
                    showError(context.getString(R.string.error_io))
                    hideProgress()
                }
            }
        }
    }

    private fun handleCopyMenuItem() {
        Utils.setClipboardContent(activity, getString(R.string.fragment_chat_clip_title),
                adapter.selectedItems.joinToString(separator = "\n", transform = { it.message }))

        context.toast(R.string.fragment_chat_clip_status)
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

    private fun showProgress() {
        if (!progress.isRefreshing) {
            progress.isEnabled = true
            progress.isRefreshing = true
        }
    }

    private fun hideProgress() {
        progress.isEnabled = false
        progress.isRefreshing = false
    }
}