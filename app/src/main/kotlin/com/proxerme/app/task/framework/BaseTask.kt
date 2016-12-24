package com.proxerme.app.task.framework

import android.support.annotation.CallSuper

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class BaseTask<in I, O>(override var successCallback: ((O) -> Unit)? = null,
                                 override var exceptionCallback: ((Exception) -> Unit)? = null) :
        Task<I, O> {

    @CallSuper
    override fun destroy() {
        successCallback = null
        exceptionCallback = null
    }

    protected fun <TI, TO> delegatedExecute(task: Task<TI, TO>, input: TI,
                                            successCallback: (TO) -> Unit,
                                            exceptionCallback: (Exception) -> Unit) {
        task.successCallback = successCallback
        task.exceptionCallback = exceptionCallback

        task.execute(input)
    }

    open protected fun start(action: () -> Unit) {
        cancel()

        action.invoke()
    }

    open protected fun finishSuccessful(result: O) {
        successCallback?.invoke(result)
    }

    open protected fun finishWithException(result: Exception) {
        exceptionCallback?.invoke(result)
    }
}