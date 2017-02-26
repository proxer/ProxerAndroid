package com.proxerme.app.task.framework

import com.proxerme.library.connection.ProxerException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class StreamedTask<I, MO, MI, O>(private val firstTask: Task<I, MO>,
                                 private val secondTask: Task<MI, O>,
                                 private val mapFunction: (MO) -> MI = {
                                     @Suppress("UNCHECKED_CAST")
                                     it as MI
                                 }, successCallback: ((O) -> Unit)? = null,
                                 exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<I, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = firstTask.isWorking || secondTask.isWorking

    init {
        firstTask.successCallback = {
            try {
                secondTask.execute(mapFunction.invoke(it))
            } catch(exception: Exception) {
                finishWithException(ProxerException(ProxerException.UNPARSABLE))
            }
        }

        firstTask.exceptionCallback = {
            finishWithException(it)
        }

        secondTask.successCallback = {
            finishSuccessful(it)
        }

        secondTask.exceptionCallback = {
            finishWithException(it)
        }
    }

    override fun execute(input: I) {
        start {
            firstTask.execute(input)
        }
    }

    override fun cancel() {
        firstTask.cancel()
        secondTask.cancel()
    }

    override fun reset() {
        firstTask.reset()
        secondTask.reset()
    }

    override fun destroy() {
        firstTask.destroy()
        secondTask.destroy()

        super.destroy()
    }

    override fun onStart(callback: () -> Unit): BaseTask<I, O> {
        firstTask.onStart(callback)
        secondTask.onStart(callback)

        return this
    }
}
