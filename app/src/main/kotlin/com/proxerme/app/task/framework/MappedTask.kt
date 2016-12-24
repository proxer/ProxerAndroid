package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MappedTask<in I, M, O>(private val task: Task<I, M>, private val mapFunction: (M) -> O,
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
                finishWithException(exception)
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
}