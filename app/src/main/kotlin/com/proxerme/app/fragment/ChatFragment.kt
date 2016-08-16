package com.proxerme.app.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.SQLException
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import butterknife.bindView
import com.klinker.android.link_builder.Link
import com.proxerme.app.R
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.ChatAdapter
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.event.ChatEvent
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.manager.UserManager
import com.proxerme.app.module.LoginModule
import com.proxerme.app.service.ChatService
import com.proxerme.app.util.Utils
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ChatFragment : MainFragment() {

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
            hideError()
            refresh()
        }
    })

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

        override fun onMentionsClick(username: String) {
            UserActivity.navigateTo(activity, username = username)
        }
    }

    private val contentRoot: ViewGroup by bindView(R.id.contentRoot)
    private val messageInput: TextInputEditText by bindView(R.id.messageInput)
    private val sendButton: Button by bindView(R.id.sendButton)
    private val list: RecyclerView by bindView(R.id.list)
    private val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    private val errorText: TextView by bindView(R.id.errorText)
    private val errorButton: Button by bindView(R.id.errorButton)

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
    fun onMessagesChanged(event: ChatEvent) {
        if (loginModule.canLoad()) {
            refresh()
        }
    }

    private fun showError(message: String, buttonMessage: String? = null,
                          onButtonClickListener: View.OnClickListener? = null) {
        contentRoot.visibility = View.INVISIBLE
        errorContainer.visibility = View.VISIBLE
        errorText.text = Utils.buildClickableText(context, message, Link.OnClickListener { link ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link + "?device=mobile")))
            } catch (exception: ActivityNotFoundException) {
                Toast.makeText(context, R.string.link_error_not_found, Toast.LENGTH_SHORT).show()
            }
        })

        if (buttonMessage == null) {
            errorButton.text = getString(R.string.error_retry)
        } else {
            errorButton.text = buttonMessage
        }

        if (onButtonClickListener == null) {
            errorButton.setOnClickListener {
                hideError()

                if (loginModule.canLoad()) {
                    refresh()
                }
            }
        } else {
            errorButton.setOnClickListener(onButtonClickListener)
        }

        clear()
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

        refreshTask = doAsync {
            try {
                val messages = context.chatDatabase.getMessages(conference.id)

                if (messages.isEmpty()) {
                    if (!StorageHelper.hasConferenceReachedEnd(conference.id) &&
                            !ChatService.isLoadingMessages(conference.id)) {
                        ChatService.loadMoreMessages(context, conference.id)
                    }
                } else {
                    uiThread {
                        adapter.replace(messages)
                    }
                }
            } catch(exception: SQLException) {
                uiThread {
                    showError("Eine Datenbankabfrage ist fehlgeschlagen. Versuche es erneut")
                }
            }
        }
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
}