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
class LoadingTask<O>(private val taskResolver: () -> ProxerRequest<O>) : BaseListenableTask<O>() {

    override val isWorking: Boolean
        get() = call != null

    private var call: ProxerCall? = null

    override fun execute(successCallback: (O) -> Unit, exceptionCallback: (Exception) -> Unit) {
        start {
            call = MainApplication.proxerConnection.execute(taskResolver.invoke(), {
                finishSuccessful(it, successCallback)
            }, {
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