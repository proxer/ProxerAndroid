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

    override fun execute(input: I) {
        delegatedExecute(task, input, {
            successCallback?.invoke(mapFunction.invoke(it))
        }, {
            exceptionCallback?.invoke(it)
        })
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