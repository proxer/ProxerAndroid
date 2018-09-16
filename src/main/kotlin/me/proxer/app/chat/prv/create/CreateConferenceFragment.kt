package me.proxer.app.chat.prv.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.editorActions
import com.jakewharton.rxbinding2.widget.textChanges
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Predicate
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.base.BaseFragment
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.message.MessengerActivity
import me.proxer.app.exception.InvalidInputException
import me.proxer.app.exception.TopicEmptyException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.colorRes
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.subscribeAndLogErrors
import org.jetbrains.anko.find
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class CreateConferenceFragment : BaseFragment() {

    companion object {
        fun newInstance() = CreateConferenceFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: CreateConferenceActivity
        get() = activity as CreateConferenceActivity

    private val viewModel by viewModel<CreateConferenceViewModel>()
    private val validators by inject<Validators>()

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

    private var innerAdapter by Delegates.notNull<CreateConferenceParticipantAdapter>()
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
    private val sendButton: ImageView by bindView(R.id.sendButton)

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

        innerAdapter = CreateConferenceParticipantAdapter(savedInstanceState)
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        innerAdapter.positionResolver = ContainerPositionResolver(adapter)

        if (savedInstanceState == null) {
            initialParticipant?.let {
                innerAdapter.add(it)

                if (!isGroup) {
                    adapter.footer = null
                }
            }
        }

        innerAdapter.removalSubject
            .autoDisposable(this.scope())
            .subscribe {
                if (adapter.footer == null) {
                    adapter.footer = addParticipantFooter
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        addParticipantFooter = inflater.inflate(
            R.layout.item_create_conference_add_participant,
            container, false
        ) as ViewGroup

        addParticipantInputFooter = inflater.inflate(
            R.layout.item_create_conference_add_participant_input,
            container, false
        ) as ViewGroup

        addParticipantImage.setIconicsImage(
            when (isGroup) {
                true -> CommunityMaterial.Icon.cmd_account_plus
                false -> CommunityMaterial.Icon.cmd_account_multiple_plus
            }, 96, 16
        )

        acceptParticipant.setIconicsImage(CommunityMaterial.Icon.cmd_check, 48, 16)
        cancelParticipant.setIconicsImage(CommunityMaterial.Icon.cmd_close, 48, 16)

        addParticipantFooter.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                adapter.footer = addParticipantInputFooter

                addParticipantFooter.post {
                    addParticipantInputFooter.requestFocus()
                }
            }

        cancelParticipant.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                participantInput.text.clear()

                adapter.footer = addParticipantFooter

                messageInput.requestFocus()
            }

        acceptParticipant.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                if (validateAndAddUser()) {
                    messageInput.requestFocus()
                }
            }

        participantInput.textChanges()
            .skipInitialValue()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                participantInputContainer.error = null
                participantInputContainer.isErrorEnabled = false
            }

        participantInput.editorActions(Predicate { it == EditorInfo.IME_ACTION_NEXT })
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                if (it == EditorInfo.IME_ACTION_NEXT && validateAndAddUser()) {
                    messageInput.requestFocus()
                }
            }

        if (innerAdapter.itemCount <= 0) {
            adapter.footer = addParticipantInputFooter
        }

        return inflater.inflate(R.layout.fragment_create_conference, container, false)
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

        sendButton.setImageDrawable(
            IconicsDrawable(requireContext(), CommunityMaterial.Icon.cmd_send)
                .colorRes(requireContext(), R.color.accent)
                .sizeDp(32)
                .paddingDp(4)
        )

        emojiButton.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { emojiPopup.toggle() }

        sendButton.clicks()
            .map {
                validators.validateLogin()

                val topic = topicInput.text.toString()
                val firstMessage = messageInput.text.toString()
                val participants = innerAdapter.participants

                when {
                    isGroup && topic.isBlank() -> throw TopicEmptyException()
                    firstMessage.isBlank() -> throw InvalidInputException(
                        requireContext().getString(R.string.error_missing_message)
                    )
                    participants.isEmpty() -> throw InvalidInputException(
                        requireContext().getString(R.string.error_missing_participants)
                    )
                }

                Triple(topic, firstMessage, participants)
            }
            .doOnError {
                when (it) {
                    is InvalidInputException -> it.message?.let { message ->
                        multilineSnackbar(root, message)
                    }
                    is TopicEmptyException -> {
                        topicInputContainer.isErrorEnabled = true
                        topicInputContainer.error = requireContext().getString(R.string.error_input_empty)
                    }
                    else -> ErrorUtils.handle(it).let { action ->
                        multilineSnackbar(
                            root, action.message, Snackbar.LENGTH_LONG, action.buttonMessage,
                            action.toClickListener(hostingActivity)
                        )
                    }
                }
            }
            .retry()
            .observeOn(AndroidSchedulers.mainThread())
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribeAndLogErrors { (topic, firstMessage, participants) ->
                when (isGroup) {
                    true -> viewModel.createGroup(topic, firstMessage, participants)
                    false -> viewModel.createChat(firstMessage, participants.first())
                }
            }

        if (isGroup) {
            topicInput.textChanges()
                .skipInitialValue()
                .autoDisposable(viewLifecycleOwner.scope())
                .subscribe {
                    topicInputContainer.isErrorEnabled = false
                    topicInputContainer.error = null
                }

            topicInput.editorActions(Predicate { it == EditorInfo.IME_ACTION_NEXT })
                .autoDisposable(viewLifecycleOwner.scope())
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

            topicInput.requestFocus()
        } else {
            topicContainer.isGone = true

            if (innerAdapter.itemCount <= 0) {
                addParticipantInputFooter.requestFocus()
            } else {
                messageInput.requestFocus()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner, Observer {
            progress.isEnabled = it == true
            progress.isRefreshing = it == true
        })

        viewModel.result.observe(viewLifecycleOwner, Observer {
            it?.let { _ ->
                requireActivity().finish()

                MessengerActivity.navigateTo(requireActivity(), it)
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            it?.let { _ ->
                multilineSnackbar(
                    root,
                    it.message,
                    Snackbar.LENGTH_LONG,
                    it.buttonMessage,
                    it.toClickListener(hostingActivity)
                )
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun onDestroyView() {
        participants.layoutManager = null
        participants.adapter = null

        emojiPopup.dismiss()

        super.onDestroyView()
    }

    private fun validateAndAddUser(): Boolean = participantInput.text.toString().let {
        when {
            it.isBlank() -> {
                participantInputContainer.isErrorEnabled = true
                participantInputContainer.error = requireContext().getString(R.string.error_input_empty)

                false
            }
            innerAdapter.contains(it) -> {
                participantInputContainer.isErrorEnabled = true
                participantInputContainer.error = requireContext().getString(R.string.error_duplicate_participant)

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
        .iconColor(requireContext())
}
