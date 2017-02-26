package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseTask
import com.proxerme.app.util.ProxerConnectionWrapper
import com.proxerme.app.util.ProxerConnectionWrapper.ProxerTokenCall
import com.proxerme.library.connection.ProxerRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ProxerLoadingTask<I, O>(private var requestConstructor: (I) -> ProxerRequest<O>,
                              successCallback: ((O) -> Unit)? = null,
                              exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<I, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = call != null

    private var call: ProxerTokenCall<O>? = null

    override fun execute(input: I) {
        start {
            call = ProxerConnectionWrapper.exec(requestConstructor.invoke(input), {
                cancel()

                finishSuccessful(it)
            }, {
                cancel()

                finishWithException(it)
            })
        }
    }

    override fun cancel() {
        call?.cancel()
        call = null
    }

    override fun reset() {
        cancel()
    }
}
