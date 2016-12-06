package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseTask
import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ValidatingTask<O>(private val task: Task<O>, private val validateFunction: () -> Unit,
                        successCallback: ((O) -> Unit)? = null,
                        exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    private var onExceptionCallback: (() -> Unit)? = null

    fun onException(callback: () -> Unit): ValidatingTask<O> {
        return this.apply { onExceptionCallback = callback }
    }

    override fun execute() {
        try {
            validateFunction.invoke()
        } catch (exception: Exception) {
            exceptionCallback?.invoke(exception)
            onExceptionCallback?.invoke()

            return
        }

        delegatedExecute(task, {
            successCallback?.invoke(it)
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