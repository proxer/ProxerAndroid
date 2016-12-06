package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseListenableTask
import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ListeningTask<O>(private val task: Task<O>,
                       successCallback: ((O) -> Unit)? = null,
                       exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseListenableTask<O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    override fun execute() {
        start {
            delegatedExecute(task, {
                finishSuccessful(it, successCallback)
            }, {
                finishWithException(it, exceptionCallback)
            })
        }
    }

    override fun cancel() {
        task.cancel()
    }

    override fun reset() {
        task.reset()
    }

    override fun destroy() {
        task.destroy()
        super.destroy()
    }
}