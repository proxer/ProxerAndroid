package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseListenableTask
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class FutureLoadingTask<I, O>(private val requestConstructor: (I, (O) -> Unit,
                                                               (Exception) -> Unit) -> Future<O>,
                              successCallback: ((O) -> Unit)? = null,
                              exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseListenableTask<I, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = call != null

    private var call: Future<O>? = null

    override fun execute(input: I) {
        call = requestConstructor.invoke(input, {
            cancel()

            finishSuccessful(it)
        }, {
            cancel()

            finishWithException(it)
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