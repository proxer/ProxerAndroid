package me.proxer.app.task.chat

import com.rubengees.ktask.base.LeafTask
import com.rubengees.ktask.base.Task
import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.event.ChatErrorEvent
import me.proxer.app.event.ChatEvent
import me.proxer.app.helper.StorageHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * @author Ruben Gees
 */
class ConferencesTask : LeafTask<Unit, List<LocalConference>>() {

    override var isWorking = false

    override fun execute(input: Unit) {
        start {
            isWorking = true

            safelyRegister()

            try {
                if (StorageHelper.hasConferenceListReachedEnd) {
                    chatDb.getAllConferences().let {
                        isWorking = false

                        finishSuccessful(chatDb.getAllConferences())
                    }
                }
            } catch (error: Throwable) {
                finishWithError(error)
            }
        }
    }

    override fun restoreCallbacks(from: Task<Unit, List<LocalConference>>) {
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
    fun onChatSynchronization(event: ChatEvent) {
        isWorking = false

        if (event.data.isNotEmpty()) {
            finishSuccessful(event.data.keys.toList())
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