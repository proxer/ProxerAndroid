package com.proxerme.app.task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CachedTask<O>(private val task: Task<O>) : BaseTask<O>() {

    override val isWorking: Boolean
        get() = throw UnsupportedOperationException()

    private var cachedResult: O? = null
    private var cachedException: Exception? = null

    override fun execute(successCallback: (O) -> Unit, exceptionCallback: (Exception) -> Unit) {
        start {
            cachedResult?.let {
                finishSuccessful(it, successCallback)
                return@start
            }

            cachedException?.let {
                finishWithException(it, exceptionCallback)
                return@start
            }

            task.execute({
                cachedResult = it

                finishSuccessful(it, successCallback)
            }, {
                cachedException = it

                finishWithException(it, exceptionCallback)
            })
        }
    }

    override fun cancel() {
        task.cancel()
    }

    override fun reset() {
        cachedResult = null
        cachedException = null

        task.reset()
    }

    override fun destroy() {
        cachedResult = null
        cachedException = null

        task.destroy()
        super.destroy()
    }
}