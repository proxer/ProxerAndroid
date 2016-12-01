package com.proxerme.app.task

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.proxerme.app.data.ChatDatabase
import com.proxerme.app.data.chatDatabase
import org.jetbrains.anko.doAsync
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class DatabaseTask<T>(private val contextCallback: () -> Context,
                      private val call: (ChatDatabase) -> T) : BaseTask<T>() {

    override val isWorking: Boolean
        get() = !(future?.isDone ?: true)

    private val handler = Handler(Looper.getMainLooper())

    private var future: Future<Unit>? = null

    override fun execute(successCallback: (T) -> Unit,
                         exceptionCallback: (Exception) -> Unit) {
        start {
            future = doAsync {
                try {
                    val result = call.invoke(contextCallback.invoke().chatDatabase)

                    handler.post {
                        finishSuccessful(result, successCallback)
                    }
                } catch (exception: Exception) {
                    handler.post {
                        finishWithException(exception, exceptionCallback)
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
}