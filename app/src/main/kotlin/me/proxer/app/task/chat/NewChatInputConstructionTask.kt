package me.proxer.app.task.chat

import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.entity.chat.Participant
import me.proxer.app.task.chat.NewChatInputConstructionTask.NewChatTaskInput
import me.proxer.library.api.ProxerCall

/**
 * @author Ruben Gees
 */
class NewChatInputConstructionTask : WorkerTask<NewChatTaskInput, ProxerCall<String>>() {

    override fun work(input: NewChatTaskInput): ProxerCall<String> {
        return when (input.isGroup) {
            true -> api.messenger()
                    .createConferenceGroup(input.topic, input.firstMessage, input.participants.map { it.username })
                    .build()
            false -> api.messenger()
                    .createConference(input.firstMessage, input.participants.first().username)
                    .build()
        }
    }

    class NewChatTaskInput(val isGroup: Boolean, val topic: String, val firstMessage: String,
                           val participants: Collection<Participant>)
}