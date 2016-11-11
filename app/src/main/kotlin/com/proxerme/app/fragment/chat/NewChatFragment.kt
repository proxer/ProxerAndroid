package com.proxerme.app.fragment.chat

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.TextInputLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import butterknife.bindView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.proxerme.app.R
import com.proxerme.app.activity.chat.ChatActivity
import com.proxerme.app.adapter.chat.NewChatParticipantAdapter
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.entitiy.Participant
import com.proxerme.app.event.ChatSynchronizationEvent
import com.proxerme.app.fragment.framework.MainFragment
import com.proxerme.app.fragment.framework.RetainedLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.manager.UserManager
import com.proxerme.app.service.ChatService
import com.proxerme.app.util.ErrorHandler
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.messenger.request.NewConferenceRequest
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class NewChatFragment : MainFragment() {

    companion object {
        private const val PARTICIPANT_ARGUMENT = "participant"
        private const val IS_GROUP_ARGUMENT = "is_group"
        private const val NEW_PARTICIPANT_STATE = "new_chat_fragment_state_new_participant"
        private const val NEW_CONFERENCE_ID_STATE = "new_chat_fragment_state_new_conference"
        private const val LOADER_TAG = "loader"

        private const val ICON_SIZE = 32
        private const val ICON_PADDING = 8

        fun newInstance(initialParticipant: Participant? = null,
                        isGroup: Boolean = false): NewChatFragment {
            return NewChatFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(PARTICIPANT_ARGUMENT, initialParticipant)
                    putBoolean(IS_GROUP_ARGUMENT, isGroup)
                }
            }
        }
    }

    override val section = Section.NEW_CHAT

    private lateinit var loader: RetainedLoadingFragment<String>

    private lateinit var adapter: NewChatParticipantAdapter
    private lateinit var headerFooterAdapter: EasyHeaderFooterAdapter

    private var newParticipant: String? = null
    private var newConferenceId: String? = null

    private lateinit var emojiPopup: EmojiPopup

    private val root: ViewGroup by bindView(R.id.root)
    private val progress: SwipeRefreshLayout by bindView(R.id.progress)
    private val topicContainer: ViewGroup by bindView(R.id.topicContainer)
    private val topicInputContainer: TextInputLayout by bindView(R.id.topicInputContainer)
    private val topicInput: EditText by bindView(R.id.topicInput)
    private val participantList: RecyclerView by bindView(R.id.participantList)
    private val emojiButton: ImageButton by bindView(R.id.emojiButton)
    private val messageInput: EmojiEditText by bindView(R.id.messageInput)
    private val sendButton: FloatingActionButton by bindView(R.id.sendButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = NewChatParticipantAdapter(savedInstanceState)
        adapter.callback = object : NewChatParticipantAdapter.NewChatParticipantAdapterCallback {
            override fun onParticipantRemoved() {
                if (adapter.itemCount <= 0) {
                    refreshNewParticipantFooter()
                }
            }
        }

        headerFooterAdapter = EasyHeaderFooterAdapter(adapter)

        if (arguments.getParcelable<Participant>(PARTICIPANT_ARGUMENT) != null) {
            adapter.add(arguments.getParcelable(PARTICIPANT_ARGUMENT))
        }

        savedInstanceState?.let {
            newParticipant = it.getString(NEW_PARTICIPANT_STATE)
            newConferenceId = it.getString(NEW_CONFERENCE_ID_STATE)
        }

        initLoader()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_new_chat, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments.getBoolean(IS_GROUP_ARGUMENT)) {
            topicInput.addTextChangedListener(object : Utils.OnTextListener() {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    topicInputContainer.isErrorEnabled = false
                    topicInputContainer.error = null
                }
            })
        } else {
            topicContainer.visibility = View.GONE
        }

        emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon.cmd_emoticon))
        emojiButton.setOnClickListener {
            emojiPopup.toggle()

            root.viewTreeObserver.dispatchOnGlobalLayout()
        }

        sendButton.setOnClickListener {
            if (checkIfCanLoad()) {
                createChat()
            }
        }

        participantList.isNestedScrollingEnabled = false
        participantList.layoutManager = LinearLayoutManager(context)
        participantList.adapter = headerFooterAdapter

        progress.setColorSchemeResources(R.color.primary)

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

        refreshNewParticipantFooter()
    }

    override fun onResume() {
        super.onResume()

        loader.setListener({ result ->
            newConferenceId = result
            val existingConference = context.chatDatabase.getConference(result)

            if (existingConference == null) {
                if (!ChatService.isSynchronizing) {
                    ChatService.synchronize(context)
                }
            } else {
                activity.finish()
                ChatActivity.navigateTo(activity, existingConference)
            }
        }, { result ->
            Snackbar.make(root, ErrorHandler.getMessageForErrorCode(context, result),
                    Snackbar.LENGTH_LONG).show()
            hideProgress()
        })

        handleProgress()
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onDestroy() {
        loader.removeListener()

        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(NEW_PARTICIPANT_STATE, newParticipant)
        outState.putString(NEW_CONFERENCE_ID_STATE, newConferenceId)
        adapter.saveInstanceState(outState)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChatCreatedAndLoaded(@Suppress("UNUSED_PARAMETER") event: ChatSynchronizationEvent) {
        if (newConferenceId != null) {
            val conference = context.chatDatabase.getConference(newConferenceId!!)

            activity.finish()
            if (conference != null) {
                ChatActivity.navigateTo(activity, conference)
            }
        }
    }

    private fun initLoader() {
        @Suppress("UNCHECKED_CAST")
        val foundLoader = childFragmentManager.findFragmentByTag(LOADER_TAG)
                as RetainedLoadingFragment<String>?

        if (foundLoader == null) {
            loader = RetainedLoadingFragment<String>()

            childFragmentManager.beginTransaction()
                    .add(loader, LOADER_TAG)
                    .commitNow()
        } else {
            loader = foundLoader
        }
    }

    private fun checkIfCanLoad(): Boolean {
        if (loader.isLoading()) {
            return false
        }

        if (arguments.getBoolean(IS_GROUP_ARGUMENT)) {
            if (topicInput.text.isBlank()) {
                topicInputContainer.isErrorEnabled = true
                topicInputContainer.error = context.getString(R.string.error_input_empty)

                return false
            }
        }

        if (messageInput.text.isBlank()) {
            Snackbar.make(root, context.getString(R.string.error_no_message), Snackbar.LENGTH_LONG)
                    .show()

            return false
        }

        if (adapter.isEmpty()) {
            Snackbar.make(root, context.getString(R.string.error_no_users), Snackbar.LENGTH_LONG)
                    .show()

            return false
        }

        if (UserManager.loginState != UserManager.LoginState.LOGGED_IN) {
            Snackbar.make(root, context.getString(R.string.status_not_logged_in), Snackbar.LENGTH_LONG)
                    .setAction(context.getString(R.string.module_login_login), {
                        LoginDialog.show(activity as AppCompatActivity)
                    }).show()

            return false
        }

        return true
    }

    private fun generateEmojiDrawable(iconicRes: IIcon): Drawable {
        return IconicsDrawable(context)
                .icon(iconicRes)
                .sizeDp(ICON_SIZE)
                .paddingDp(ICON_PADDING)
                .colorRes(R.color.icon)
    }

    private fun createChat() {
        val request: NewConferenceRequest

        if (arguments.getBoolean(IS_GROUP_ARGUMENT)) {
            request = NewConferenceRequest(topicInput.text.toString().trim(),
                    adapter.participants.map { it.username })
        } else {
            request = NewConferenceRequest(adapter.participants.first().username)
        }

        if (messageInput.text.isNotBlank()) {
            request.withFirstMessage(messageInput.text.toString().trim())
        }

        loader.load(request)
        handleProgress()
    }

    private fun refreshNewParticipantFooter() {
        if (!arguments.getBoolean(IS_GROUP_ARGUMENT) && adapter.itemCount >= 1) {
            newParticipant = null
            headerFooterAdapter.removeFooter()

            return
        }

        if (newParticipant == null) {
            val addParticipantItem = LayoutInflater.from(context)
                    .inflate(R.layout.item_add_participant, root, false)
            val image: ImageView = addParticipantItem.findViewById(R.id.image) as ImageView

            addParticipantItem.setOnClickListener {
                newParticipant = ""

                refreshNewParticipantFooter()
            }

            image.setImageDrawable(IconicsDrawable(image.context)
                    .icon(if (arguments.getBoolean(IS_GROUP_ARGUMENT))
                        CommunityMaterial.Icon.cmd_account_plus else
                        CommunityMaterial.Icon.cmd_account_multiple_plus)
                    .sizeDp(96)
                    .paddingDp(16)
                    .colorRes(R.color.icon))

            headerFooterAdapter.setFooter(addParticipantItem)
        } else {
            val addParticipantInputItem = LayoutInflater.from(context)
                    .inflate(R.layout.item_add_participant_input, root, false)
            val inputContainer: TextInputLayout =
                    addParticipantInputItem.findViewById(R.id.participantInputContainer)
                            as TextInputLayout
            val input: EditText = addParticipantInputItem.findViewById(R.id.participantInput)
                    as EditText
            val accept: ImageButton = addParticipantInputItem.findViewById(R.id.accept)
                    as ImageButton
            val cancel: ImageButton = addParticipantInputItem.findViewById(R.id.cancel)
                    as ImageButton

            input.setText(newParticipant)
            input.addTextChangedListener(object : Utils.OnTextListener() {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    newParticipant = input.text.toString()

                    inputContainer.error = null
                    inputContainer.isErrorEnabled = false
                }
            })
            input.requestFocus()

            accept.setImageDrawable(IconicsDrawable(accept.context)
                    .icon(CommunityMaterial.Icon.cmd_check)
                    .sizeDp(48)
                    .paddingDp(16)
                    .colorRes(R.color.icon))
            accept.setOnClickListener {
                if (input.text.isBlank()) {
                    inputContainer.isErrorEnabled = true
                    inputContainer.error = context.getString(R.string.error_input_empty)
                } else if (adapter.contains(input.text.toString())) {
                    inputContainer.isErrorEnabled = true
                    inputContainer.error = context.getString(R.string.error_duplicate_user)
                } else {
                    adapter.add(Participant(input.text.toString(), ""))
                    newParticipant = ""

                    input.text.clear()

                    if (!arguments.getBoolean(IS_GROUP_ARGUMENT) && adapter.itemCount >= 1) {
                        refreshNewParticipantFooter()
                    }
                }
            }

            cancel.setImageDrawable(IconicsDrawable(accept.context)
                    .icon(CommunityMaterial.Icon.cmd_close)
                    .sizeDp(48)
                    .paddingDp(16)
                    .colorRes(R.color.icon))
            cancel.setOnClickListener {
                newParticipant = null

                refreshNewParticipantFooter()
            }

            headerFooterAdapter.setFooter(addParticipantInputItem)
        }
    }

    private fun showProgress() {
        progress.isEnabled = true
        progress.isRefreshing = true
    }

    private fun hideProgress() {
        progress.isEnabled = false
        progress.isRefreshing = false
    }

    private fun handleProgress() {
        if (isLoading()) {
            showProgress()
        } else {
            hideProgress()
        }
    }

    private fun isLoading(): Boolean {
        return loader.isLoading() || if (newConferenceId == null) false else
            ChatService.isSynchronizing
    }
}