package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseListenableTask
import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ListeningTask<I, O>(private val task: Task<I, O>,
                          successCallback: ((O) -> Unit)? = null,
                          exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseListenableTask<I, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    init {
        task.successCallback = {
            finishSuccessful(it)
        }

        task.exceptionCallback = {
            finishWithException(it)
        }
    }

    override fun execute(input: I) {
        start {
            task.execute(input)
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