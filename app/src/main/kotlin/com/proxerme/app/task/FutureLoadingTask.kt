package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseListenableTask
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class FutureLoadingTask<O>(private val requestResolver: ((O) -> Unit, (Exception) -> Unit) -> Future<O>,
                           successCallback: ((O) -> Unit)? = null,
                           exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseListenableTask<O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = call != null

    private var call: Future<O>? = null

    override fun execute() {
        call = requestResolver.invoke({
            cancel()

            finishSuccessful(it, successCallback)
        }, {
            cancel()

            finishWithException(it, exceptionCallback)
        })
    }

    override fun cancel() {
        call?.cancel(true)
        call = null
    }

    override fun reset() {
        cancel()
    }
}