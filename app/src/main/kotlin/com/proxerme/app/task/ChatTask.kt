package com.proxerme.app.task

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.event.ChatMessagesEvent
import com.proxerme.app.fragment.framework.PagedLoadingFragment.PagedInput
import com.proxerme.app.service.ChatService
import com.proxerme.app.service.ChatService.LoadMoreMessagesException
import com.proxerme.app.task.ChatTask.ChatInput
import com.proxerme.app.task.framework.BaseTask
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
class ChatTask(private val id: String,
               private var contextResolver: (() -> Context)? = null,
               successCallback: ((Array<LocalMessage>) -> Unit)? = null,
               exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<ChatInput, Array<LocalMessage>>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = !(future?.isDone ?: true) || ChatService.isLoadingMessages(id)

    private val handler = Handler(Looper.getMainLooper())

    private var future: Future<Unit>? = null

    init {
        EventBus.getDefault().register(this)
    }

    override fun execute(input: ChatInput) {
        start {
            if (input.page == 0) {
                future = doAsync {
                    try {
                        input.context.chatDatabase.markAsRead(id)

                        val result = input.context.chatDatabase.getMessages(id)

                        if (result.isEmpty()) {
                            if (!ChatService.isLoadingMessages(id)) {
                                ChatService.loadMoreMessages(input.context, id)
                            }
                        } else {
                            handler.post {
                                finishSuccessful(result.toTypedArray())
                            }
                        }
                    } catch (exception: Exception) {
                        handler.post {
                            finishWithException(exception)
                        }
                    }
                }
            } else if (!ChatService.isLoadingMessages(id)) {
                ChatService.loadMoreMessages(input.context, id)
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
    fun onLoadMessages(event: ChatMessagesEvent) {
        if (id == event.conferenceId && event.messages.isNotEmpty()) {
            finishSuccessful(event.messages.toTypedArray())
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoadMessagesFailed(exception: LoadMoreMessagesException) {
        if (id == exception.conferenceId) {
            finishWithException(exception)
        }
    }

    class ChatInput(page: Int, val context: Context) : PagedInput(page)
}