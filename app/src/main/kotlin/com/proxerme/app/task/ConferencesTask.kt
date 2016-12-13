package com.proxerme.app.task

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.event.ChatSynchronizationEvent
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.task.framework.BaseListenableTask
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ConferencesTask(successCallback: ((List<LocalConference>) -> Unit)? = null,
                      exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseListenableTask<Context, List<LocalConference>>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = !(future?.isDone ?: true)

    private val handler = Handler(Looper.getMainLooper())
    private var future: Future<Unit>? = null

    init {
        EventBus.getDefault().register(this)
    }

    override fun execute(input: Context) {
        start {
            if (StorageHelper.conferenceListEndReached) {
                future = doAsync {
                    try {
                        val result = input.chatDatabase.getConferences()

                        handler.post {
                            finishSuccessful(result)
                        }
                    } catch (exception: Exception) {
                        handler.post {
                            finishWithException(exception)
                        }
                    }
                }
            }
        }
    }

    override fun cancel() {
        future?.cancel(true)
        future = null
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
        finishSuccessful(event.newEntryMap.keys.toList())
    }
}