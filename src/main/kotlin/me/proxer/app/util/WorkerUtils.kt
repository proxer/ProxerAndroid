package me.proxer.app.util

object WorkerUtils {

    fun shouldShowError(runAttemptCount: Int, error: Throwable) = when {
        ErrorUtils.isIpBlockedError(error) || ErrorUtils.isNotConnectedError(error) -> true
        runAttemptCount >= 2 && !ErrorUtils.isNetworkError(error) -> true
        else -> false
    }
}
