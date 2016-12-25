package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ValidatingTask<I, O>(private val task: Task<I, O>,
                           private val validationFunction: (I) -> Unit,
                           successCallback: ((O) -> Unit)? = null,
                           exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<I, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    init {
        task.successCallback = {
            finishSuccessful(it)
        }

        task.exceptionCallback = {
            finishWithException(it)
        }
    }

    override fun execute(input: I) {
        try {
            validationFunction.invoke(input)
        } catch (exception: Exception) {
            finishWithException(exception)

            return
        }

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