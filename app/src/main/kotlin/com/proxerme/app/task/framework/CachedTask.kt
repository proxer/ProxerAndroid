package com.proxerme.app.task.framework

import com.proxerme.app.task.framework.CachedTask.CacheStrategy

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CachedTask<I, O>(private val task: Task<I, O>,
                       cacheStrategy: CacheStrategy = CacheStrategy.FULL,
                       successCallback: ((O) -> Unit)? = null,
                       exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<I, O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    private val shouldCachedResult = cacheStrategy == CacheStrategy.FULL ||
            cacheStrategy == CacheStrategy.RESULT
    private val shouldCacheException = cacheStrategy == CacheStrategy.FULL ||
            cacheStrategy == CacheStrategy.EXCEPTION

    var cachedResult: O? = null
        private set
    var cachedException: Exception? = null
        private set

    init {
        task.successCallback = {
            cachedResult = if (shouldCachedResult) it else null

            finishSuccessful(it)
        }

        task.exceptionCallback = {
            cachedException = if (shouldCacheException) it else null

            finishWithException(it)
        }
    }

    override fun execute(input: I) {
        if (shouldCachedResult) {
            cachedResult?.let {
                finishSuccessful(it)

                return
            }

            if (isWorking) {
                return
            }
        }

        if (shouldCacheException) {
            cachedException?.let {
                finishWithException(it)

                return
            }

            if (isWorking) {
                return
            }
        }

        start {
            task.execute(input)
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

    override fun onStart(callback: () -> Unit): BaseTask<I, O> {
        task.onStart(callback)

        return this
    }

    fun mutate(operation: (O?) -> O?) {
        cachedResult = operation.invoke(cachedResult)
    }

    fun clear(strategy: CacheStrategy) = when (strategy) {
        CacheStrategy.FULL -> {
            cachedResult = null
            cachedException = null
        }
        CacheStrategy.RESULT -> cachedResult = null
        CacheStrategy.EXCEPTION -> cachedException = null
    }

    enum class CacheStrategy {FULL, RESULT, EXCEPTION }
}