package com.proxerme.app.task.framework

import android.support.annotation.CallSuper

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class BaseListenableTask<I, O>(successCallback: ((O) -> Unit)? = null,
                                        exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<I, O>(successCallback, exceptionCallback), ListenableTask<I, O> {

    private var onStartCallback: (() -> Unit)? = null
    private var onSuccessCallback: (() -> Unit)? = null
    private var onExceptionCallback: (() -> Unit)? = null
    private var onFinishCallback: (() -> Unit)? = null

    override fun onStart(callback: () -> Unit): ListenableTask<I, O> {
        return this.apply { onStartCallback = callback }
    }

    override fun onSuccess(callback: () -> Unit): ListenableTask<I, O> {
        return this.apply { onSuccessCallback = callback }
    }

    override fun onException(callback: () -> Unit): ListenableTask<I, O> {
        return this.apply { onExceptionCallback = callback }
    }

    override fun onFinish(callback: () -> Unit): ListenableTask<I, O> {
        return this.apply { onFinishCallback = callback }
    }

    @CallSuper
    override fun destroy() {
        onStartCallback = null
        onSuccessCallback = null
        onExceptionCallback = null
        onFinishCallback = null

        super.destroy()
    }

    protected fun start(action: () -> Unit) {
        onStartCallback?.invoke()

        action.invoke()
    }

    override fun finishSuccessful(result: O) {
        super.finishSuccessful(result)

        onSuccessCallback?.invoke()
        onFinishCallback?.invoke()
    }

    override fun finishWithException(result: Exception) {
        super.finishWithException(result)

        onExceptionCallback?.invoke()
        onFinishCallback?.invoke()
    }
}