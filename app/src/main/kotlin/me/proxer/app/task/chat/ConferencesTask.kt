package me.proxer.app.task.chat

import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.event.chat.ChatErrorEvent
import me.proxer.app.event.chat.ChatSynchronizationEvent
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.EventBusTask
import org.greenrobot.eventbus.Subscribe

/**
 * @author Ruben Gees
 */
class ConferencesTask : EventBusTask<Unit, List<LocalConference>>() {

    override var isWorking = false

    override fun execute(input: Unit) {
        start {
            isWorking = true

            safelyRegister()

            try {
                if (StorageHelper.hasConferenceListReachedEnd) {
                    chatDb.getAllConferences().let {
                        isWorking = false

                        finishSuccessful(it)
                    }
                }
            } catch (error: Throwable) {
                finishWithError(error)
            }
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronization(event: ChatSynchronizationEvent) {
        isCancelled = false
        isWorking = false

        if (event.items.isNotEmpty()) {
            finishSuccessful(event.items.map { it.conference })
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronizationFailed(event: ChatErrorEvent) {
        isCancelled = false
        isWorking = false

        finishWithError(event.error)
    }
}