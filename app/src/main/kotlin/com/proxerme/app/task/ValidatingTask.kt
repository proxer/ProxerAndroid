package com.proxerme.app.task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ValidatingTask<O>(private val task: Task<O>, private val validateFunction: () -> Unit) :
        BaseTask<O>() {

    override val isWorking: Boolean
        get() = task.isWorking

    override fun execute(successCallback: (O) -> Unit, exceptionCallback: (Exception) -> Unit) {
        start {
            try {
                validateFunction.invoke()
            } catch (exception: Exception) {
                finishWithException(exception, exceptionCallback)
                return@start
            }

            task.execute({
                finishSuccessful(it, successCallback)
            }, {
                finishWithException(it, exceptionCallback)
            })
        }
    }

    override fun destroy() {
        task.destroy()
        super.destroy()
    }

    override fun cancel() {
        task.cancel()
    }

    override fun reset() {
        task.reset()
    }
}