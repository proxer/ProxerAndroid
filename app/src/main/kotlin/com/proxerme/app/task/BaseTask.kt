package com.proxerme.app.task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class BaseTask<O> : Task<O> {

    open protected var onStartCallback: (() -> Unit)? = null
    open protected var onSuccessCallback: (() -> Unit)? = null
    open protected var onExceptionCallback: (() -> Unit)? = null
    open protected var onFinishCallback: (() -> Unit)? = null

    override fun onStart(callback: () -> Unit): Task<O> {
        return this.apply { onStartCallback = callback }
    }

    override fun onSuccess(callback: () -> Unit): Task<O> {
        return this.apply { onSuccessCallback = callback }
    }

    override fun onException(callback: () -> Unit): Task<O> {
        return this.apply { onExceptionCallback = callback }
    }

    override fun onFinish(callback: () -> Unit): Task<O> {
        return this.apply { onFinishCallback = callback }
    }

    override fun destroy() {
        onStartCallback = null
        onSuccessCallback = null
        onExceptionCallback = null
        onFinishCallback = null
    }

    protected fun start(action: () -> Unit) {
        onStartCallback?.invoke()

        action.invoke()
    }

    protected fun finishSuccessful(result: O, successCallback: (O) -> Unit) {
        cancel()
        successCallback.invoke(result)

        onSuccessCallback?.invoke()
        onFinishCallback?.invoke()
    }

    protected fun finishWithException(result: Exception, exceptionCallback: (Exception) -> Unit) {
        cancel()
        exceptionCallback.invoke(result)

        onExceptionCallback?.invoke()
        onFinishCallback?.invoke()
    }
}