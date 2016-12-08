package com.proxerme.app.task

import android.content.Context
import com.proxerme.app.application.MainApplication
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.entitiy.Participant
import com.proxerme.app.event.ChatSynchronizationEvent
import com.proxerme.app.service.ChatService
import com.proxerme.app.task.NewChatTask.NewChatInput
import com.proxerme.app.task.framework.BaseListenableTask
import com.proxerme.library.connection.ProxerCall
import com.proxerme.library.connection.messenger.request.NewConferenceRequest
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class NewChatTask(private var contextResolver: (() -> Context)? = null,
                  successCallback: ((LocalConference?) -> Unit)? = null,
                  exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseListenableTask<NewChatInput, LocalConference?>(successCallback, exceptionCallback) {

    override var isWorking: Boolean = false
    private var newConferenceId: String? = null

    private var call: ProxerCall? = null

    init {
        EventBus.getDefault().register(this)
    }

    override fun execute(input: NewChatInput) {
        isWorking = true

        start {
            call = MainApplication.proxerConnection.execute(constructRequest(input), {
                contextResolver?.invoke()?.let { ChatService.synchronize(it) }

                val existingConference = contextResolver?.invoke()?.chatDatabase?.getConference(it)

                if (existingConference != null) {
                    isWorking = false

                    finishSuccessful(existingConference)
                } else {
                    newConferenceId = it
                }
            }, {
                isWorking = false

                finishWithException(it)
            })
        }
    }

    override fun cancel() {
        call?.cancel()
        call = null
        isWorking = false
    }

    override fun reset() {
        newConferenceId = null

        cancel()
    }

    override fun destroy() {
        EventBus.getDefault().unregister(this)
        reset()

        contextResolver = null

        super.destroy()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChatCreatedAndLoaded(@Suppress("UNUSED_PARAMETER") event: ChatSynchronizationEvent) {
        newConferenceId?.let {
            isWorking = false
            newConferenceId = null

            finishSuccessful(contextResolver?.invoke()?.chatDatabase?.getConference(it))
        }
    }

    private fun constructRequest(input: NewChatInput) = when (input.isGroup) {
        true -> {
            NewConferenceRequest(input.topic, input.participants
                    .map { it.username })
                    .withFirstMessage(input.firstMessage)
        }
        false -> {
            NewConferenceRequest(input.participants.first().username)
                    .withFirstMessage(input.firstMessage)
        }
    }

    class NewChatInput(val isGroup: Boolean, val topic: String, val participants: List<Participant>,
                       val firstMessage: String)
}