package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseTask
import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ValidatingTask<I, O>(private val task: Task<I, O>, private val validateFunction: () -> Unit,
                           successCallback: ((O) -> Unit)? = null,
                           exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<I, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    private var onExceptionCallback: (() -> Unit)? = null

    init {
        task.successCallback = {
            finishSuccessful(it)
        }

        task.exceptionCallback = {
            finishWithException(it)
        }
    }

    fun onException(callback: () -> Unit): ValidatingTask<I, O> {
        return this.apply { onExceptionCallback = callback }
    }

    override fun execute(input: I) {
        try {
            validateFunction.invoke()
        } catch (exception: Exception) {
            exceptionCallback?.invoke(exception)
            onExceptionCallback?.invoke()

            return
        }

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