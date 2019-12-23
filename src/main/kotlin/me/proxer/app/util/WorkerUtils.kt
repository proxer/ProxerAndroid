package me.proxer.app.util

object WorkerUtils {

    fun shouldRetryForError(error: Throwable) = when {
        ErrorUtils.isIpBlockedError(error) -> false
        else -> true
    }
}
