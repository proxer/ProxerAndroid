package me.proxer.app.task.chat

import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.event.chat.ChatErrorEvent
import me.proxer.app.event.chat.ChatSynchronizationEvent
import me.proxer.app.job.ChatJob
import me.proxer.app.task.EventBusTask
import org.greenrobot.eventbus.Subscribe

/**
 * @author Ruben Gees
 */
class NewChatAwaitTask : EventBusTask<String, LocalConference>() {

    override var isWorking = false

    private var currentInput: String? = null

    override fun execute(input: String) {
        start {
            isWorking = true
            currentInput = input

            safelyRegister()
            ChatJob.scheduleSynchronization()
        }
    }

    override fun cancel() {
        super.cancel()

        isWorking = false
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronization(event: ChatSynchronizationEvent) {
        isCancelled = false
        isWorking = false

        event.items.filter { it.conference.id == currentInput }.let {
            if (it.isNotEmpty()) {
                finishSuccessful(it.first().conference)
            }
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronizationFailed(event: ChatErrorEvent) {
        if (isWorking) {
            isCancelled = false
            isWorking = false

            finishWithError(event.error)
        }
    }
}