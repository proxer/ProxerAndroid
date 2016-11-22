package com.proxerme.app.task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MappedTask<I, O>(private val task: Task<I>, private val mapFunction: (I) -> O) :
        BaseTask<O>() {

    override val isWorking: Boolean
        get() = task.isWorking

    override fun execute(successCallback: (O) -> Unit, exceptionCallback: (Exception) -> Unit) {
        start {
            task.execute({
                finishSuccessful(mapFunction.invoke(it), successCallback)
            }, {
                finishWithException(it, exceptionCallback)
            })
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