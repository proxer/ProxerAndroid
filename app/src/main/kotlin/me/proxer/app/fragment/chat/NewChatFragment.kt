package me.proxer.app.fragment.chat

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcel
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar.LENGTH_LONG
import android.support.design.widget.TextInputLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import me.proxer.app.R
import me.proxer.app.activity.ChatActivity
import me.proxer.app.activity.NewChatActivity
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.adapter.chat.NewChatParticipantAdapter
import me.proxer.app.adapter.chat.NewChatParticipantAdapter.NewChatParticipantAdapterCallback
import me.proxer.app.application.GlideApp
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.entity.chat.Participant
import me.proxer.app.fragment.base.MainFragment
import me.proxer.app.task.chat.NewChatInputConstructionTask
import me.proxer.app.task.chat.NewChatInputConstructionTask.NewChatTaskInput
import me.proxer.app.task.proxerTask
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.listener.TextWatcherWrapper
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class NewChatFragment : MainFragment() {

    companion object {
        fun newInstance(): NewChatFragment {
            return NewChatFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    private val newChatActivity
        get() = activity as NewChatActivity

    private val isGroup: Boolean
        get() = newChatActivity.isGroup

    private val initialParticipant: Participant?
        get() = newChatActivity.initialParticipant

    private lateinit var task: AndroidLifecycleTask<NewChatTaskInput, LocalConference>

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

    private lateinit var innerAdapter: NewChatParticipantAdapter
    private lateinit var adapter: EasyHeaderFooterAdapter

    private lateinit var addParticipantFooter: ViewGroup
    private lateinit var addParticipantInputFooter: ViewGroup

    private val root: ViewGroup by bindView(R.id.root)
    private val progress: SwipeRefreshLayout by bindView(R.id.progress)
    private val topicContainer: ViewGroup by bindView(R.id.topicContainer)
    private val topicInputContainer: TextInputLayout by bindView(R.id.topicInputContainer)
    private val topicInput: EditText by bindView(R.id.topicInput)
    private val participants: RecyclerView by bindView(R.id.participants)
    private val emojiButton: ImageButton by bindView(R.id.emojiButton)
    private val messageInput: EmojiEditText by bindView(R.id.messageInput)
    private val sendButton: FloatingActionButton by bindView(R.id.sendButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        task = TaskBuilder.task(NewChatInputConstructionTask())
                .then(TaskBuilder.proxerTask<NewChatTaskInput>())
                .then(ToDoTask())
                .validateBefore {
                    Validators.validateLogin()
                    Validators.validateNewChatInput(context, it)
                }
                .bindToLifecycle(this)
                .onInnerStart {
                    setProgressVisible(true)
                }
                .onSuccess {
                    activity.finish()

                    ChatActivity.navigateTo(activity, it)
                }
                .onError { error ->
                    when (error) {
                        is Validators.InvalidInputException -> {
                            error.message?.let {
                                multilineSnackbar(root, it)
                            }
                        }
                        is Validators.TopicEmptyException -> {
                            topicInputContainer.isErrorEnabled = true
                            topicInputContainer.error = context.getString(R.string.error_input_empty)
                        }
                        else -> {
                            ErrorUtils.handle(activity as MainActivity, error).let { it ->
                                multilineSnackbar(root, it.message, LENGTH_LONG, it.buttonMessage, it.buttonAction)
                            }
                        }
                    }
                }
                .onFinish {
                    setProgressVisible(false)
                }
                .build()

        innerAdapter = NewChatParticipantAdapter(savedInstanceState, GlideApp.with(this))
        innerAdapter.callback = object : NewChatParticipantAdapterCallback {
            override fun onParticipantRemoved() {
                if (!adapter.hasFooter()) {
                    adapter.footer = addParticipantFooter
                }
            }
        }

        adapter = EasyHeaderFooterAdapter(innerAdapter)

        if (savedInstanceState == null) {
            initialParticipant?.let {
                innerAdapter.insert(listOf(it))
            }
        }
    }

    /**
     * TODO
     */
    private class ToDoTask : WorkerTask<String, LocalConference>() {
        override fun work(input: String): LocalConference {
            return LocalConference(Parcel.obtain())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        addParticipantFooter = inflater.inflate(R.layout.item_new_chat_add_participant, container, false) as ViewGroup
        addParticipantInputFooter = inflater.inflate(R.layout.item_new_chat_add_participant_input, container, false)
                as ViewGroup

        addParticipantFooter.setOnClickListener {
            adapter.footer = addParticipantInputFooter
        }

        addParticipantFooter.find<ImageView>(R.id.image).setImageDrawable(IconicsDrawable(context)
                .icon(if (isGroup) CommunityMaterial.Icon.cmd_account_plus else
                    CommunityMaterial.Icon.cmd_account_multiple_plus)
                .sizeDp(96)
                .paddingDp(16)
                .colorRes(R.color.icon))

        val inputContainer = addParticipantInputFooter.find<TextInputLayout>(R.id.participantInputContainer)
        val input = addParticipantInputFooter.find<EditText>(R.id.participantInput).apply {
            addTextChangedListener(object : TextWatcherWrapper {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    inputContainer.error = null
                    inputContainer.isErrorEnabled = false
                }
            })

            requestFocus()
        }

        addParticipantInputFooter.find<ImageButton>(R.id.accept).apply {
            setImageDrawable(IconicsDrawable(context)
                    .icon(CommunityMaterial.Icon.cmd_check)
                    .sizeDp(48)
                    .paddingDp(16)
                    .colorRes(R.color.icon))

            setOnClickListener {
                input.text.toString().let {
                    when {
                        it.isBlank() -> {
                            inputContainer.isErrorEnabled = true
                            inputContainer.error = context.getString(R.string.error_input_empty)
                        }
                        innerAdapter.contains(it) -> {
                            inputContainer.isErrorEnabled = true
                            inputContainer.error = context.getString(R.string.error_duplicate_participant)
                        }
                        else -> {
                            innerAdapter.append(listOf(Participant(it, "")))

                            input.text.clear()

                            if (!isGroup && innerAdapter.itemCount >= 1) {
                                adapter.removeFooter()
                            }
                        }
                    }
                }
            }
        }

        addParticipantInputFooter.find<ImageButton>(R.id.cancel).apply {
            setImageDrawable(IconicsDrawable(context)
                    .icon(CommunityMaterial.Icon.cmd_close)
                    .sizeDp(48)
                    .paddingDp(16)
                    .colorRes(R.color.icon))

            setOnClickListener {
                input.text.clear()

                adapter.footer = addParticipantFooter
            }
        }

        adapter.footer = addParticipantFooter

        return inflater.inflate(R.layout.fragment_new_chat, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isGroup) {
            topicInput.addTextChangedListener(object : TextWatcherWrapper {
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
        }

        sendButton.setOnClickListener {
            if (!task.isWorking) {
                task.execute(NewChatTaskInput(isGroup, topicInput.text.toString(), messageInput.text.toString(),
                        innerAdapter.list))
            }
        }

        participants.isNestedScrollingEnabled = false
        participants.layoutManager = LinearLayoutManager(context)
        participants.adapter = adapter

        progress.setColorSchemeResources(R.color.primary)

        setProgressVisible(task.isWorking)
    }

    private fun setProgressVisible(enable: Boolean) {
        progress.isEnabled = enable
        progress.isRefreshing = enable
    }

    private fun generateEmojiDrawable(iconicRes: IIcon): Drawable {
        return IconicsDrawable(context)
                .icon(iconicRes)
                .sizeDp(32)
                .paddingDp(6)
                .colorRes(R.color.icon)
    }
}
