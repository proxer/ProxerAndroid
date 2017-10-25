package me.proxer.app.chat.create

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.TextInputLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.editorActions
import com.jakewharton.rxbinding2.widget.textChanges
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Predicate
import io.reactivex.schedulers.Schedulers
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseFragment
import me.proxer.app.chat.ChatActivity
import me.proxer.app.chat.Participant
import me.proxer.app.exception.InvalidInputException
import me.proxer.app.exception.TopicEmptyException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class CreateChatFragment : BaseFragment() {

    companion object {
        fun newInstance() = CreateChatFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: CreateChatActivity
        get() = activity as CreateChatActivity

    private val viewModel by unsafeLazy { CreateChatViewModelProvider.get(this) }

    private val isGroup: Boolean
        get() = hostingActivity.isGroup

    private val initialParticipant: Participant?
        get() = hostingActivity.initialParticipant

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

    private var innerAdapter by Delegates.notNull<CreateChatParticipantAdapter>()
    private var adapter by Delegates.notNull<EasyHeaderFooterAdapter>()

    private var addParticipantFooter by Delegates.notNull<ViewGroup>()
    private var addParticipantInputFooter by Delegates.notNull<ViewGroup>()

    private val root: ViewGroup by bindView(R.id.root)
    private val progress: SwipeRefreshLayout by bindView(R.id.progress)
    private val topicContainer: ViewGroup by bindView(R.id.topicContainer)
    private val topicInputContainer: TextInputLayout by bindView(R.id.topicInputContainer)
    private val topicInput: EditText by bindView(R.id.topicInput)
    private val participants: RecyclerView by bindView(R.id.participants)
    private val emojiButton: ImageButton by bindView(R.id.emojiButton)
    private val messageInput: EmojiEditText by bindView(R.id.messageInput)
    private val sendButton: FloatingActionButton by bindView(R.id.sendButton)

    private val addParticipantImage by lazy {
        addParticipantFooter.find<ImageView>(R.id.image)
    }

    private val participantInputContainer by lazy {
        addParticipantInputFooter.find<TextInputLayout>(R.id.participantInputContainer)
    }

    private val participantInput by lazy {
        addParticipantInputFooter.find<EditText>(R.id.participantInput)
    }

    private val acceptParticipant by lazy {
        addParticipantInputFooter.find<ImageButton>(R.id.accept)
    }

    private val cancelParticipant by lazy {
        addParticipantInputFooter.find<ImageButton>(R.id.cancel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = CreateChatParticipantAdapter(savedInstanceState)
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        innerAdapter.removalSubject
                .autoDispose(this)
                .subscribe {
                    if (adapter.footer == null) {
                        adapter.footer = addParticipantFooter
                    }
                }

        viewModel.isLoading.observe(this, Observer {
            progress.isEnabled = it == true
            progress.isRefreshing = it == true
        })

        viewModel.result.observe(this, Observer {
            it?.let {
                safeActivity.finish()

                ChatActivity.navigateTo(safeActivity, it)
            }
        })

        viewModel.error.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, it.message, Snackbar.LENGTH_LONG, it.buttonMessage,
                        it.buttonAction?.toClickListener(hostingActivity))
            }
        })

        if (savedInstanceState == null) {
            initialParticipant?.let {
                innerAdapter.add(it)

                if (!isGroup) {
                    adapter.footer = null
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        addParticipantFooter = inflater.inflate(R.layout.item_create_chat_add_participant,
                container, false) as ViewGroup

        addParticipantInputFooter = inflater.inflate(R.layout.item_create_chat_add_participant_input,
                container, false) as ViewGroup

        addParticipantImage.setImageDrawable(IconicsDrawable(context)
                .icon(when {
                    isGroup -> CommunityMaterial.Icon.cmd_account_plus
                    else -> CommunityMaterial.Icon.cmd_account_multiple_plus
                })
                .sizeDp(96)
                .paddingDp(16)
                .colorRes(R.color.icon))

        acceptParticipant.setImageDrawable(IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_check)
                .sizeDp(48)
                .paddingDp(16)
                .colorRes(R.color.icon))

        cancelParticipant.setImageDrawable(IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_close)
                .sizeDp(48)
                .paddingDp(16)
                .colorRes(R.color.icon))

        addParticipantFooter.clicks()
                .autoDispose(this)
                .subscribe { adapter.footer = addParticipantInputFooter }

        cancelParticipant.clicks()
                .autoDispose(this)
                .subscribe {
                    participantInput.text.clear()

                    adapter.footer = addParticipantFooter

                    messageInput.requestFocus()
                }

        acceptParticipant.clicks()
                .autoDispose(this)
                .subscribe {
                    if (validateAndAddUser()) {
                        messageInput.requestFocus()
                    }
                }

        participantInput.textChanges()
                .skipInitialValue()
                .autoDispose(this)
                .subscribe {
                    participantInputContainer.error = null
                    participantInputContainer.isErrorEnabled = false
                }

        participantInput.editorActions(Predicate { it == EditorInfo.IME_ACTION_NEXT })
                .autoDispose(this)
                .subscribe {
                    if (it == EditorInfo.IME_ACTION_NEXT && validateAndAddUser()) {
                        messageInput.requestFocus()
                    }
                }

        if (isGroup || innerAdapter.itemCount <= 0) {
            adapter.footer = addParticipantFooter
        }

        return inflater.inflate(R.layout.fragment_create_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)

        participants.isNestedScrollingEnabled = false
        participants.layoutManager = LinearLayoutManager(context)
        participants.adapter = adapter

        progress.setColorSchemeResources(R.color.primary)
        progress.isEnabled = false

        emojiButton.setImageDrawable(generateEmojiDrawable(CommunityMaterial.Icon.cmd_emoticon))

        emojiButton.clicks()
                .autoDispose(this)
                .subscribe { emojiPopup.toggle() }

        sendButton.clicks()
                .flatMap {
                    Observable.fromCallable {
                        Validators.validateLogin()

                        val topic = topicInput.text.toString()
                        val firstMessage = messageInput.text.toString()
                        val participants = innerAdapter.participants

                        when {
                            isGroup && topic.isBlank() -> throw TopicEmptyException()
                            firstMessage.isBlank() -> throw InvalidInputException(safeContext
                                    .getString(R.string.error_missing_message))
                            participants.isEmpty() -> throw InvalidInputException(safeContext
                                    .getString(R.string.error_missing_participants))
                        }

                        Triple(topic, firstMessage, participants)
                    }.subscribeOn(Schedulers.io())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(this)
                .subscribeAndLogErrors({ (topic, firstMessage, participants) ->
                    when (isGroup) {
                        true -> viewModel.createGroup(topic, firstMessage, participants)
                        false -> viewModel.createChat(firstMessage, participants.first())
                    }
                }, {
                    when (it) {
                        is InvalidInputException -> it.message?.let {
                            multilineSnackbar(root, it)
                        }
                        is TopicEmptyException -> {
                            topicInputContainer.isErrorEnabled = true
                            topicInputContainer.error = safeContext.getString(R.string.error_input_empty)
                        }
                        else -> ErrorUtils.handle(it).let { action ->
                            multilineSnackbar(root, action.message, Snackbar.LENGTH_LONG, action.buttonMessage,
                                    action.buttonAction?.toClickListener(hostingActivity))
                        }
                    }
                })

        if (isGroup) {
            topicInput.textChanges()
                    .skipInitialValue()
                    .autoDispose(this)
                    .subscribe {
                        topicInputContainer.isErrorEnabled = false
                        topicInputContainer.error = null
                    }

            topicInput.editorActions(Predicate { it == EditorInfo.IME_ACTION_NEXT })
                    .autoDispose(this)
                    .subscribe {
                        if (it == EditorInfo.IME_ACTION_NEXT) {
                            if (innerAdapter.isEmpty()) {
                                adapter.footer = addParticipantInputFooter

                                participantInput.requestFocus()
                            } else {
                                messageInput.requestFocus()
                            }
                        }
                    }
        } else {
            topicContainer.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        participants.layoutManager = null
        participants.adapter = null

        emojiPopup.dismiss()

        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    private fun validateAndAddUser(): Boolean = participantInput.text.toString().let {
        when {
            it.isBlank() -> {
                participantInputContainer.isErrorEnabled = true
                participantInputContainer.error = safeContext.getString(R.string.error_input_empty)

                false
            }
            innerAdapter.contains(it) -> {
                participantInputContainer.isErrorEnabled = true
                participantInputContainer.error = safeContext.getString(R.string.error_duplicate_participant)

                false
            }
            else -> {
                innerAdapter.add(Participant(it, ""))

                participantInput.text.clear()

                if (!isGroup && innerAdapter.itemCount >= 1) {
                    adapter.footer = null

                    true
                } else {
                    false
                }
            }
        }
    }

    private fun generateEmojiDrawable(iconicRes: IIcon) = IconicsDrawable(context)
            .icon(iconicRes)
            .sizeDp(32)
            .paddingDp(6)
            .colorRes(R.color.icon)
}
