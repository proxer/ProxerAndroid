package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ZippedTask<I, I2, M, M2, O>(private val firstTask: Task<I, M>,
                                  private val secondTask: Task<I2, M2>,
                                  private val zipFunction: (M, M2) -> O,
                                  successCallback: ((O) -> Unit)? = null,
                                  exceptionCallback: ((Exception) -> Unit)? = null,
                                  private val awaitFirstResult: Boolean = false,
                                  private val awaitSecondResult: Boolean = false) :
        BaseTask<Pair<I, I2>, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = firstTask.isWorking || secondTask.isWorking

    private var firstResult: M? = null
    private var secondResult: M2? = null

    private var firstError: Exception? = null
    private var secondError: Exception? = null

    init {
        firstTask.successCallback = {
            if (secondResult != null) {
                finishSuccessful(zipFunction.invoke(it, secondResult!!))
                reset()
            } else if (secondError != null) {
                finishWithException(PartialException(secondError!!, it!!))
                reset()
            } else {
                firstResult = it
            }
        }

        firstTask.exceptionCallback = {
            if (awaitSecondResult) {
                if (secondResult != null) {
                    finishWithException(PartialException(it, secondResult!!))
                    reset()
                } else {
                    firstError = it
                }
            } else {
                cancel()
                finishWithException(it)
                reset()
            }
        }

        secondTask.successCallback = {
            if (firstResult != null) {
                finishSuccessful(zipFunction.invoke(firstResult!!, it))
                reset()
            } else if (firstError != null) {
                finishWithException(PartialException(firstError!!, it!!))
                reset()
            } else {
                secondResult = it
            }
        }

        secondTask.exceptionCallback = {
            if (awaitFirstResult) {
                if (firstResult != null) {
                    finishWithException(PartialException(it, firstResult!!))
                    reset()
                } else {
                    secondError = it
                }
            } else {
                cancel()
                finishWithException(it)
                reset()
            }
        }
    }

    override fun execute(input: Pair<I, I2>) {
        start {
            firstTask.execute(input.first)
            secondTask.execute(input.second)
        }
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
        firstError = null
        secondError = null
    }

    override fun destroy() {
        firstTask.destroy()
        secondTask.destroy()

        super.destroy()
    }

    override fun onStart(callback: () -> Unit): BaseTask<Pair<I, I2>, O> {
        firstTask.onStart(callback)
        secondTask.onStart(callback)

        return this
    }

    class PartialException(val original: Exception, val data: Any) : Exception()
}