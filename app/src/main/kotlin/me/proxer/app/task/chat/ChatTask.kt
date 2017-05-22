package me.proxer.app.task.chat

import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.LocalConferenceAssociation
import me.proxer.app.event.chat.ChatErrorEvent
import me.proxer.app.event.chat.ChatMessageEvent
import me.proxer.app.job.ChatJob
import me.proxer.app.job.ChatJob.ChatMessageException
import me.proxer.app.task.EventBusTask
import org.greenrobot.eventbus.Subscribe

/**
 * @author Ruben Gees
 */
class ChatTask(private val id: String) : EventBusTask<Int, LocalConferenceAssociation>() {

    override var isWorking = false

    override fun execute(input: Int) {
        start {
            isWorking = true

            safelyRegister()

            try {
                if (input == 0) {
                    chatDb.markAsRead(id)
                    chatDb.getAllMessages(id).let { messages ->
                        chatDb.getConference(id).let {
                            if (messages.isEmpty() && !it.isFullyLoaded) {
                                ChatJob.scheduleMessageLoad(id)
                            } else {
                                isWorking = false

                                finishSuccessful(LocalConferenceAssociation(it, messages))
                            }
                        }
                    }
                } else {
                    ChatJob.scheduleMessageLoad(id)
                }
            } catch (error: Throwable) {
                finishWithError(error)
            }
        }
    }

    override fun cancel() {
        isWorking = false
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronization(event: ChatMessageEvent) {
        isCancelled = false
        isWorking = false

        if (event.item.conference.id == id) {
            finishSuccessful(event.item)
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronizationFailed(event: ChatErrorEvent) {
        if (event.error is ChatMessageException) {
            isCancelled = false
            isWorking = false

            finishWithError(event.error)
        }
    }
}