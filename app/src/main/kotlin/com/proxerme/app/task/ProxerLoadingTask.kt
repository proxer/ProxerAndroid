package com.proxerme.app.task

import com.proxerme.app.application.MainApplication
import com.proxerme.app.task.framework.BaseListenableTask
import com.proxerme.library.connection.ProxerCall
import com.proxerme.library.connection.ProxerRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ProxerLoadingTask<O>(private var requestResolver: () -> ProxerRequest<O>,
                           successCallback: ((O) -> Unit)? = null,
                           exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseListenableTask<O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = call != null

    private var call: ProxerCall? = null

    override fun execute() {
        start {
            call = MainApplication.proxerConnection.execute(requestResolver.invoke(), {
                cancel()

                finishSuccessful(it, successCallback)
            }, {
                cancel()

                finishWithException(it, exceptionCallback)
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