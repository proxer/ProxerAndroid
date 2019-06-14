package me.proxer.app.util

object WorkerUtils {

    fun shouldShowError(runAttemptCount: Int, error: Throwable) = when {
        ErrorUtils.isIpBlockedError(error) -> true
        runAttemptCount >= 2 && !ErrorUtils.isNetworkError(error) && !ErrorUtils.isNotConnectedError(error) -> true
        else -> false
    }
}
