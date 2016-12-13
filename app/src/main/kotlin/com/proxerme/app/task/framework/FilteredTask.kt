package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class FilteredTask<in I, O>(private val task: Task<I, O>, private val filterFunction: (O) -> O,
                            successCallback: ((O) -> Unit)? = null,
                            exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<I, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    init {
        task.successCallback = {
            try {
                finishSuccessful(filterFunction.invoke(it))
            } catch(exception: Exception) {
                finishWithException(exception)
            }
        }

        task.exceptionCallback = {
            finishWithException(it)
        }
    }

    override fun execute(input: I) {
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