package com.proxerme.app.task.framework

import android.support.annotation.CallSuper

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class BaseTask<O>(override var successCallback: ((O) -> Unit)? = null,
                           override var exceptionCallback: ((Exception) -> Unit)? = null) : Task<O> {

    @CallSuper
    override fun destroy() {
        successCallback = null
        exceptionCallback = null
    }

    protected fun <T> delegatedExecute(task: Task<T>, successCallback: (T) -> Unit,
                                       exceptionCallback: (Exception) -> Unit) {
        task.successCallback = successCallback
        task.exceptionCallback = exceptionCallback

        task.execute()
    }
}