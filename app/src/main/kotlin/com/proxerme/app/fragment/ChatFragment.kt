package com.proxerme.app.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.activity.ImageDetailActivity
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.ChatAdapter
import com.proxerme.app.application.MainApplication
import com.proxerme.app.event.MessageSentEvent
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.job.SendMessageJob
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.manager.UserManager
import com.proxerme.app.module.LoginModule
import com.proxerme.app.module.PollingModule
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.experimental.chat.entity.Conference
import com.proxerme.library.connection.experimental.chat.entity.Message
import com.proxerme.library.connection.experimental.chat.request.ChatRequest
import com.proxerme.library.info.ProxerTag
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.interfaces.ProxerErrorResult
import com.proxerme.library.interfaces.ProxerResult
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ChatFragment : PagingFragment() {

    companion object {
        private const val ARGUMENT_CONFERENCE = "conference"
        private const val POLLING_INTERVAL = 3000L

        fun newInstance(conference: Conference): ChatFragment {

            return ChatFragment().apply {
                this.arguments = Bundle().apply {
                    this.putParcelable(ARGUMENT_CONFERENCE, conference)
                }
            }
        }
    }

    private lateinit var conference: Conference

    override val section: SectionManager.Section = SectionManager.Section.CHAT

    override lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: ChatAdapter

    private val loginModule = LoginModule(object : LoginModule.LoginModuleCallback {
        override val activity: AppCompatActivity
            get() = this@ChatFragment.activity as AppCompatActivity

        override fun showError(message: String, buttonMessage: String?,
                               onButtonClickListener: View.OnClickListener?) {
            this@ChatFragment.showError(message, buttonMessage, onButtonClickListener)
        }

        override fun load(showProgress: Boolean) {
            this@ChatFragment.load(showProgress)
        }
    })

    override val canLoad: Boolean
        get() = super.canLoad && loginModule.canLoad()
    override val loadAlways = true

    private val pollingModule = PollingModule(object : PollingModule.PollingModuleCallback {
        override val isLoading: Boolean
            get() = this@ChatFragment.isLoading
        override val canLoad: Boolean
            get() = this@ChatFragment.canLoad && currentError == null

        override fun load(showProgress: Boolean) {
            this@ChatFragment.load(showProgress)
        }
    }, POLLING_INTERVAL)

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Utils.setStatusBarColorIfPossible(activity, R.color.primary)

            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.copy -> handleCopyMenuItem()
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
        override fun onMessageImageClick(v: View, message: Message) {
            if (!message.imageId.isBlank()) {
                ImageDetailActivity.navigateTo(activity, v as ImageView,
                        ProxerUrlHolder.getUserImageUrl(message.imageId))
            }
        }

        override fun onMessageTitleClick(v: View, message: Message) {
            UserActivity.navigateTo(activity, message.fromId, message.username,
                    message.imageId)
        }

        override fun onMessageSelection(count: Int) {
            if (count > 0) {
                if (actionMode == null) {
                    actionMode = (activity as AppCompatActivity)
                            .startSupportActionMode(actionModeCallback)
                    actionMode?.title = count.toString()
                } else {
                    actionMode?.title = count.toString()
                }
            } else {
                actionMode?.finish()
            }
        }

        override fun onMessageLinkClick(link: String) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            } catch (exception: ActivityNotFoundException) {
                Toast.makeText(context, R.string.link_error_not_found, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val contentRoot: ViewGroup by bindView(R.id.contentRoot)
    private val messageInput: TextInputEditText by bindView(R.id.messageInput)
    private val sendButton: Button by bindView(R.id.sendButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.cancelNotification(context, NotificationHelper.CHAT_NOTIFICATION)

        conference = arguments.getParcelable(ARGUMENT_CONFERENCE)

        adapter = ChatAdapter(savedInstanceState)
        adapter.user = UserManager.user
        adapter.callback = adapterCallback

        layoutManager = LinearLayoutManager(context)
        layoutManager.reverseLayout = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = adapter

        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()

            if (!text.isEmpty()) {
                MainApplication.jobManager.addJobInBackground(SendMessageJob(conference.id, text))
            }

            messageInput.text.clear()
        }
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
        pollingModule.onResume()
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
        loginModule.onStart()
    }

    override fun onPause() {
        pollingModule.onPause()

        super.onPause()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        loginModule.onStop()

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?,
                             savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun showError(message: String, buttonMessage: String?,
                           onButtonClickListener: View.OnClickListener?) {
        super.showError(message, buttonMessage, onButtonClickListener)

        contentRoot.visibility = View.INVISIBLE
    }

    override fun hideError() {
        super.hideError()

        contentRoot.visibility = View.VISIBLE
        adapter.user = UserManager.user
    }

    override fun loadPage(number: Int) {
        ChatRequest(conference.id, number).execute({ result ->
            if (result.item.isNotEmpty()) {
                adapter.addItems(result.item.asList())

                if (number == firstPage) {
                    StorageHelper.lastMessageTime = result.item.first().time
                    StorageHelper.newMessages = 0
                    StorageHelper.resetMessagesInterval()
                    NotificationHelper.retrieveChatLater(context)
                }
            }

            notifyPagedLoadFinishedSuccessful(number, result)
        }, { result ->
            notifyPagedLoadFinishedWithError(number, result)
        })
    }

    override fun notifyLoadFinishedSuccessful(result: ProxerResult<*>) {
        super.notifyLoadFinishedSuccessful(result)

        pollingModule.onSuccessfulRequest()
    }

    override fun notifyLoadFinishedWithError(result: ProxerErrorResult) {
        super.notifyLoadFinishedWithError(result)

        pollingModule.onErrorRequest()
    }

    override fun clear() {
        adapter.clear()
        adapter.user = null
    }

    override fun cancel() {
        ProxerConnection.cancel(ProxerTag.CHAT)
    }

    private fun handleCopyMenuItem() {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(getString(R.string.fragment_chat_clip_title),
                adapter.selectedItems.joinToString(separator = "\n", transform = { it.message }))

        clipboard.primaryClip = clip

        Toast.makeText(context, R.string.fragment_chat_clip_status, Toast.LENGTH_SHORT).show()
        actionMode?.finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageSent(event: MessageSentEvent) {
        if (event.conferenceId == conference.id && !isLoading && canLoad) {
            load(false)
        }
    }
}