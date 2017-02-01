package com.proxerme.app.task.framework

import com.proxerme.library.connection.ProxerException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MappedTask<I, M, O>(private val task: Task<I, M>,
                          private val mapFunction: (M) -> O,
                          successCallback: ((O) -> Unit)? = null,
                          exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<I, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    init {
        task.successCallback = {
            try {
                finishSuccessful(mapFunction.invoke(it))
            } catch(exception: Exception) {
                when (exception) {
                    is ProxerException -> finishWithException(exception)
                    else -> finishWithException(ProxerException(ProxerException.UNPARSABLE))
                }
            }
        }

        task.exceptionCallback = {
            finishWithException(it)
        }
    }

    override fun execute(input: I) {
        start {
            task.execute(input)
        }
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

    override fun onStart(callback: () -> Unit): BaseTask<I, O> {
        task.onStart(callback)

        return this
    }
}