package com.proxerme.app.task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ZippedTask<O, O2, O3>(private val firstTask: Task<O>, private val secondTask: Task<O2>,
                            private val zipFunction: (O, O2) -> O3) : BaseTask<O3>() {

    override val isWorking: Boolean
        get() = firstTask.isWorking || secondTask.isWorking

    override fun execute(successCallback: (O3) -> Unit, exceptionCallback: (Exception) -> Unit) {
        onStartCallback?.invoke()

        var firstResult: O? = null
        var secondResult: O2? = null

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
}