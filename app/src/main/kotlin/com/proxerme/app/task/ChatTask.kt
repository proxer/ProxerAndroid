package com.proxerme.app.task

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.event.ChatMessagesEvent
import com.proxerme.app.service.ChatService
import com.proxerme.app.service.ChatService.LoadMoreMessagesException
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
class ChatTask(private val contextCallback: () -> Context,
               private val refreshOnlyCallback: () -> Boolean,
               private val id: String, successCallback: ((Array<LocalMessage>) -> Unit)? = null,
               exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseListenableTask<Array<LocalMessage>>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = !(future?.isDone ?: true) || ChatService.isLoadingMessages(id)

    private val handler = Handler(Looper.getMainLooper())

    private var future: Future<Unit>? = null

    init {
        EventBus.getDefault().register(this)
    }

    override fun execute() {
        start {
            if (refreshOnlyCallback.invoke()) {
                future = doAsync {
                    try {
                        contextCallback.invoke().chatDatabase.markAsRead(id)

                        val result = contextCallback.invoke().chatDatabase.getMessages(id)

                        if (result.isEmpty()) {
                            if (!ChatService.isLoadingMessages(id)) {
                                ChatService.loadMoreMessages(contextCallback.invoke(), id)
                            }
                        } else {
                            handler.post {
                                finishSuccessful(result.toTypedArray(), successCallback)
                            }
                        }
                    } catch (exception: Exception) {
                        handler.post {
                            finishWithException(exception, exceptionCallback)
                        }
                    }
                }
            } else if (!ChatService.isLoadingMessages(id)) {
                ChatService.loadMoreMessages(contextCallback.invoke(), id)
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
    fun onLoadMessages(@Suppress("UNUSED_PARAMETER") event: ChatMessagesEvent) {
        successCallback?.let {
            if (event.messages.isNotEmpty()) {
                finishSuccessful(event.messages.toTypedArray(), it)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoadMessagesFailed(@Suppress("UNUSED_PARAMETER") exception: LoadMoreMessagesException) {
        exceptionCallback?.let {
            finishWithException(exception, it)
        }
    }

}