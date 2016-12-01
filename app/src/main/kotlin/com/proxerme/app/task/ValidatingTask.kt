package com.proxerme.app.task

import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ValidatingTask<O>(private val task: Task<O>, private val validateFunction: () -> Unit) :
        Task<O> {

    override val isWorking: Boolean
        get() = task.isWorking

    override fun execute(successCallback: (O) -> Unit, exceptionCallback: (Exception) -> Unit) {
        try {
            validateFunction.invoke()
        } catch (exception: Exception) {
            exceptionCallback.invoke(exception)

            return
        }

        task.execute({
            successCallback.invoke(it)
        }, {
            exceptionCallback.invoke(it)
        })
    }

    override fun destroy() {
        task.destroy()
    }

    override fun cancel() {
        task.cancel()
    }

    override fun reset() {
        task.reset()
    }
}