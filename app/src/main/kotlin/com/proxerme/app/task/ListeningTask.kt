package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseListenableTask
import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ListeningTask<O>(private val task: Task<O>) : BaseListenableTask<O>() {

    override val isWorking: Boolean
        get() = task.isWorking

    override fun execute(successCallback: (O) -> Unit, exceptionCallback: (Exception) -> Unit) {
        start {
            task.execute({
                finishSuccessful(it, successCallback)
            }, {
                finishWithException(it, exceptionCallback)
            })
        }
    }

    override fun destroy() {
        task.destroy()

        super.destroy()
    }

    override fun cancel() {
        task.cancel()
    }

    override fun reset() {
        task.reset()
    }
}