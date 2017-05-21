package me.proxer.app.task.chat

import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.LocalConferenceAssociation
import me.proxer.app.event.chat.ChatSynchronizationEvent
import me.proxer.app.task.EventBusTask
import org.greenrobot.eventbus.Subscribe

/**
 * @author Ruben Gees
 */
class ChatRefreshTask(private val id: String) : EventBusTask<Int, LocalConferenceAssociation>() {

    override val isWorking = false

    override fun execute(input: Int) {
        start {
            safelyRegister()
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronization(event: ChatSynchronizationEvent) {
        event.items.filter { it.conference.id == id }.firstOrNull()?.let {
            isCancelled = false

            chatDb.markAsRead(id)

            finishSuccessful(it)
        }
    }
}