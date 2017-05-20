package me.proxer.app.task.chat

import com.rubengees.ktask.base.LeafTask
import com.rubengees.ktask.base.Task
import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.entity.chat.LocalMessage
import me.proxer.app.event.ChatErrorEvent
import me.proxer.app.event.ChatMessageEvent
import me.proxer.app.job.ChatJob
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * @author Ruben Gees
 */
class ChatTask(private val id: String) : LeafTask<Int, Pair<LocalConference, List<LocalMessage>>>() {

    override var isWorking = false

    override fun execute(input: Int) {
        start {
            isWorking = true

            safelyRegister()

            try {
                if (input == 0) {
                    chatDb.getAllMessages(id).let { messages ->
                        chatDb.getConference(id).let {
                            if (messages.isEmpty() && !it.isLoadedFully) {
                                ChatJob.scheduleMessageLoad(id)
                            } else {
                                isWorking = false

                                finishSuccessful(it to messages)
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

    override fun restoreCallbacks(from: Task<Int, Pair<LocalConference, List<LocalMessage>>>) {
        super.restoreCallbacks(from)

        safelyRegister()
    }

    override fun retainingDestroy() {
        EventBus.getDefault().unregister(this)

        super.retainingDestroy()
    }

    override fun destroy() {
        EventBus.getDefault().unregister(this)

        super.destroy()
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronization(event: ChatMessageEvent) {
        isWorking = false

        if (event.conference.id == id) {
            finishSuccessful(event.conference to event.messages)
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onChatSynchronizationFailed(event: ChatErrorEvent) {
        isWorking = false

        finishWithError(event.error)
    }

    private fun safelyRegister() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }
}