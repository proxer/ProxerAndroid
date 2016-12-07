package com.proxerme.app.task

import com.proxerme.app.task.ZippedTask.ZippedInput
import com.proxerme.app.task.framework.BaseTask
import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ZippedTask<I, I2, M, M2, O>(private val firstTask: Task<I, M>,
                                  private val secondTask: Task<I2, M2>,
                                  private val zipFunction: (M, M2) -> O,
                                  successCallback: ((O) -> Unit)? = null,
                                  exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<ZippedInput<I, I2>, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = firstTask.isWorking || secondTask.isWorking

    override fun execute(input: ZippedInput<I, I2>) {
        var firstResult: M? = null
        var secondResult: M2? = null

        delegatedExecute(firstTask, input.first, { result ->
            secondResult?.let {
                successCallback?.invoke(zipFunction.invoke(result, it))

                return@let
            }

            firstResult = result
        }, {
            exceptionCallback?.invoke(it)
        })

        delegatedExecute(secondTask, input.second, { result ->
            firstResult?.let {
                successCallback?.invoke(zipFunction.invoke(it, result))

                return@let
            }

            secondResult = result
        }, {
            exceptionCallback?.invoke(it)
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

    class ZippedInput<out I, out I2>(val first: I, val second: I2)
}