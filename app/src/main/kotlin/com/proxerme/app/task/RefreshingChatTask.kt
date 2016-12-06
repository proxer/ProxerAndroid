package com.proxerme.app.task

import android.content.Context
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.event.ChatSynchronizationEvent
import com.proxerme.app.task.framework.BaseListenableTask
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class RefreshingChatTask(private val id: String, private val contextCallback: () -> Context,
                         successCallback: ((Array<LocalMessage>) -> Unit)? = null,
                         exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseListenableTask<Array<LocalMessage>>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = false

    init {
        EventBus.getDefault().register(this)
    }

    override fun execute() {

    }

    override fun cancel() {

    }

    override fun reset() {
        cancel()
    }

    override fun destroy() {
        EventBus.getDefault().unregister(this)
        reset()

        super.destroy()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSynchronization(@Suppress("UNUSED_PARAMETER") event: ChatSynchronizationEvent) {
        successCallback?.let {
            val relevantEntries = event.newEntryMap.entries.filter { it.key.id == id }

            if (relevantEntries.isNotEmpty()) {
                contextCallback.invoke().chatDatabase.markAsRead(id)

                it.invoke(relevantEntries.flatMap { it.value }.toTypedArray())
            }
        }
    }
}