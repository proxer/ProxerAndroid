package com.proxerme.app.task

import com.proxerme.app.task.framework.BaseTask
import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ZippedTask<I, I2, O>(private val firstTask: Task<I>, private val secondTask: Task<I2>,
                           private val zipFunction: (I, I2) -> O,
                           successCallback: ((O) -> Unit)? = null,
                           exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = firstTask.isWorking || secondTask.isWorking

    override fun execute() {
        var firstResult: I? = null
        var secondResult: I2? = null

        delegatedExecute(firstTask, { result ->
            secondResult?.let {
                successCallback?.invoke(zipFunction.invoke(result, it))

                return@let
            }

            firstResult = result
        }, {
            exceptionCallback?.invoke(it)
        })

        delegatedExecute(secondTask, { result ->
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
}