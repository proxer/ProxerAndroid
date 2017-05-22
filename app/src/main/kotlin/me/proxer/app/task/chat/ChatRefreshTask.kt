package me.proxer.app.task.chat

import com.rubengees.ktask.base.Task
import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.LocalConferenceAssociation
import me.proxer.app.event.chat.ChatErrorEvent
import me.proxer.app.event.chat.ChatSynchronizationEvent
import me.proxer.app.job.ChatJob.ChatSendMessageException
import me.proxer.app.job.ChatJob.ChatSynchronizationException
import me.proxer.app.task.EventBusTask
import org.greenrobot.eventbus.Subscribe

/**
 * @author Ruben Gees
 */
class ChatRefreshTask(private val id: String, chatErrorCallback: ChatErrorCallback)
    : EventBusTask<Int, LocalConferenceAssociation>() {

    override val isWorking = false

    private var chatErrorCallback: ChatErrorCallback? = chatErrorCallback

    override fun execute(input: Int) {
        start {
            safelyRegister()
        }
    }

    override fun retainingDestroy() {
        super.retainingDestroy()

        chatErrorCallback = null
    }

    override fun restoreCallbacks(from: Task<Int, LocalConferenceAssociation>) {
        super.restoreCallbacks(from)

        if (from !is ChatRefreshTask) {
            throw IllegalArgumentException("The passed task must have the same type.")
        }

        chatErrorCallback = from.chatErrorCallback
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronization(event: ChatSynchronizationEvent) {
        event.items.filter { it.conference.id == id }.firstOrNull()?.let {
            isCancelled = false

            chatDb.markAsRead(id)

            if (it.messages.isNotEmpty()) {
                finishSuccessful(it)
            }
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronizationFailed(event: ChatErrorEvent) {
        if (event.error is ChatSynchronizationException) {
            isCancelled = false

            if (event.error.innerError is ChatSendMessageException) {
                chatErrorCallback?.messageSendFailed(event.error.innerError.id)
            }
        }
    }

    interface ChatErrorCallback {
        fun messageSendFailed(id: String)
    }
}