package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ZippedTask<in I, in I2, M, M2, O>(private val firstTask: Task<I, M>,
                                        private val secondTask: Task<I2, M2>,
                                        private val zipFunction: (M, M2) -> O,
                                        successCallback: ((O) -> Unit)? = null,
                                        exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<Pair<I, I2>, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = firstTask.isWorking || secondTask.isWorking

    private var firstResult: M? = null
    private var secondResult: M2? = null

    init {
        firstTask.successCallback = { result ->
            secondResult?.let {
                finishSuccessful(zipFunction.invoke(result, it))
                reset()

                return@let
            }

            firstResult = result
        }

        firstTask.exceptionCallback = {
            finishWithException(it)
            reset()
        }

        secondTask.successCallback = { result ->
            firstResult?.let {
                finishSuccessful(zipFunction.invoke(it, result))
                reset()

                return@let
            }

            secondResult = result
        }

        secondTask.exceptionCallback = {
            finishWithException(it)
            cancel()
        }
    }

    override fun execute(input: Pair<I, I2>) {
        firstTask.execute(input.first)
        secondTask.execute(input.second)
    }

    override fun cancel() {
        firstTask.cancel()
        secondTask.cancel()
    }

    override fun reset() {
        firstTask.reset()
        secondTask.reset()

        firstResult = null
        secondResult = null
    }

    override fun destroy() {
        firstTask.destroy()
        secondTask.destroy()

        super.destroy()
    }
}