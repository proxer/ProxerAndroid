package com.proxerme.app.task.framework

import android.support.annotation.CallSuper

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class BaseTask<I, O>(override var successCallback: ((O) -> Unit)? = null,
                              override var exceptionCallback: ((Exception) -> Unit)? = null) :
        Task<I, O> {

    private var onStartCallback: (() -> Unit)? = null
    private var onSuccessCallback: (() -> Unit)? = null
    private var onExceptionCallback: (() -> Unit)? = null
    private var onFinishCallback: (() -> Unit)? = null

    override fun onStart(callback: () -> Unit): BaseTask<I, O> {
        return this.apply { onStartCallback = callback }
    }

    override fun onSuccess(callback: () -> Unit): BaseTask<I, O> {
        return this.apply { onSuccessCallback = callback }
    }

    override fun onException(callback: () -> Unit): BaseTask<I, O> {
        return this.apply { onExceptionCallback = callback }
    }

    override fun onFinish(callback: () -> Unit): BaseTask<I, O> {
        return this.apply { onFinishCallback = callback }
    }

    @CallSuper
    override fun destroy() {
        successCallback = null
        exceptionCallback = null

        onStartCallback = null
        onSuccessCallback = null
        onExceptionCallback = null
        onFinishCallback = null
    }

    open protected fun start(action: () -> Unit) {
        cancel()

        startWithoutCancelling(action)
    }

    open protected fun startWithoutCancelling(action: () -> Unit) {
        onStartCallback?.invoke()

        action.invoke()
    }

    open protected fun finishSuccessful(result: O) {
        successCallback?.invoke(result)

        onSuccessCallback?.invoke()
        onFinishCallback?.invoke()
    }

    open protected fun finishWithException(result: Exception) {
        exceptionCallback?.invoke(result)

        onExceptionCallback?.invoke()
        onFinishCallback?.invoke()
    }

    protected fun <TI, TO> delegatedExecute(task: Task<TI, TO>, input: TI,
                                            successCallback: (TO) -> Unit,
                                            exceptionCallback: (Exception) -> Unit) {
        task.successCallback = successCallback
        task.exceptionCallback = exceptionCallback

        task.execute(input)
    }
}