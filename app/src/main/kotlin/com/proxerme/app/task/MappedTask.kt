package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseTask
import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MappedTask<I, M, O>(private val task: Task<I, M>, private val mapFunction: (M) -> O,
                          successCallback: ((O) -> Unit)? = null,
                          exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<I, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    init {
        task.successCallback = {
            super.successCallback?.invoke(mapFunction.invoke(it))
        }

        task.exceptionCallback = {
            super.exceptionCallback?.invoke(it)
        }
    }

    override fun execute(input: I) {
        task.execute(input)
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