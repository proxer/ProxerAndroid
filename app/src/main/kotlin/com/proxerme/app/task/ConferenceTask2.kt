package com.proxerme.app.task

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.event.ChatSynchronizationEvent
import com.proxerme.app.service.ChatService
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
class ConferenceTask2(private val contextCallback: () -> Context,
                      private val onlyRefreshCallback: () -> Boolean) :
        BaseTask<Array<LocalConference>>() {

    override var isWorking: Boolean = false
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var future: Future<Unit>? = null

    private var successCallback: ((Array<LocalConference>) -> Unit)? = null
    private var exceptionCallback: ((Exception) -> Unit)? = null

    init {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun execute(successCallback: (Array<LocalConference>) -> Unit,
                         exceptionCallback: (Exception) -> Unit) {
        start {
            this.isWorking = true
            this.successCallback = successCallback
            this.exceptionCallback = exceptionCallback
            val onlyRefresh = onlyRefreshCallback.invoke()

            if (onlyRefresh) {
                future = doAsync {
                    try {
                        val existingConferences = contextCallback.invoke().chatDatabase
                                .getConferences()

                        if (existingConferences.isEmpty()) {
                            ChatService.loadMoreConferences(contextCallback.invoke())
                        } else {
                            handler.post {
                                isWorking = false

                                finishSuccessful(existingConferences.toTypedArray(), successCallback)
                            }
                        }
                    } catch (exception: Exception) {
                        handler.post {
                            isWorking = false

                            finishWithException(exception, exceptionCallback)
                        }
                    }
                }
            } else {
                ChatService.loadMoreConferences(contextCallback.invoke())
            }
        }
    }

    @Suppress("unused")
    @Subscribe(priority = 1)
    fun onConferencesChanged(@Suppress("UNUSED_PARAMETER") event: ChatSynchronizationEvent) {
        if (isWorking) {
            EventBus.getDefault().cancelEventDelivery(event)
            isWorking = false

            handler.post {
                successCallback?.let {
                    finishSuccessful(event.conferences.toTypedArray(), it)
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoadMoreConferencesFailed(exception: ChatService.LoadMoreConferencesException) {
        isWorking = false

        handler.post {
            exceptionCallback?.let {
                finishWithException(exception, it)
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

        super.destroy()
    }
}