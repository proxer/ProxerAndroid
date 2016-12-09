package com.proxerme.app.task

import android.os.Handler
import android.os.Looper
import com.proxerme.app.stream.StreamResolverFactory
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.task.framework.BaseListenableTask
import org.jetbrains.anko.doAsync
import java.io.IOException
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class StreamResolutionTask(successCallback: ((StreamResolutionResult) -> Unit)? = null,
                           exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseListenableTask<String, StreamResolutionResult>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = !(future?.isDone ?: true)

    private val handler = Handler(Looper.getMainLooper())
    private var future: Future<Unit>? = null

    override fun execute(input: String) {
        start {
            future = doAsync {
                try {
                    val result = StreamResolverFactory.getResolverFor(input)?.resolve(input)
                            ?: throw IOException()

                    cancel()

                    handler.post {
                        finishSuccessful(result)
                    }
                } catch (exception: Exception) {
                    cancel()

                    handler.post {
                        finishWithException(exception)
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
        reset()

        super.destroy()
    }

    class StreamResolutionResult(val url: String, val mimeType: String)
}