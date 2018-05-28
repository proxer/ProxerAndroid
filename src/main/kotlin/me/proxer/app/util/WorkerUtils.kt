package me.proxer.app.util

object WorkerUtils {

    fun shouldShowError(error: Throwable) = when {
        ErrorUtils.isIpBlockedError(error) -> true
        !ErrorUtils.isNetworkError(error) -> true
        else -> false
    }
}
