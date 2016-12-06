package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseTask
import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MappedTask<I, O>(private val task: Task<I>, private val mapFunction: (I) -> O,
                       successCallback: ((O) -> Unit)? = null,
                       exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    override fun execute() {
        delegatedExecute(task, {
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