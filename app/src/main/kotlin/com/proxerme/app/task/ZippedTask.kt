package com.proxerme.app.task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ZippedTask<I, I2, O>(private val firstTask: Task<I>, private val secondTask: Task<I2>,
                           private val zipFunction: (I, I2) -> O) : BaseTask<O>() {

    override val isWorking: Boolean
        get() = firstTask.isWorking || secondTask.isWorking

    override fun execute(successCallback: (O) -> Unit, exceptionCallback: (Exception) -> Unit) {
        onStartCallback?.invoke()

        var firstResult: I? = null
        var secondResult: I2? = null

        firstTask.execute({ result ->
            secondResult?.let {
                finishSuccessful(zipFunction.invoke(result, it), successCallback)

                return@let
            }

            firstResult = result
        }, {
            finishWithException(it, exceptionCallback)
        })

        secondTask.execute({ result ->
            firstResult?.let {
                finishSuccessful(zipFunction.invoke(it, result), successCallback)

                return@let
            }

            secondResult = result
        }, {
            finishWithException(it, exceptionCallback)
        })
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
}