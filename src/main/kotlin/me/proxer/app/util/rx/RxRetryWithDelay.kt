package me.proxer.app.util.rx

import io.reactivex.Flowable
import io.reactivex.functions.Function
import java.util.concurrent.TimeUnit

/**
 * Stolen and adjusted from here: https://stackoverflow.com/a/25292833/4279995
 */
class RxRetryWithDelay(
    private val maxRetries: Int,
    private val retryDelayMillis: Long
) : Function<Flowable<out Throwable>, Flowable<*>> {

    private var retryCount: Int = 0

    override fun apply(attempts: Flowable<out Throwable>): Flowable<Any> = attempts
        .flatMap { throwable ->
            when {
                ++retryCount < maxRetries -> Flowable.timer(retryDelayMillis, TimeUnit.MILLISECONDS)
                else -> Flowable.error(throwable)
            }
        }
}
