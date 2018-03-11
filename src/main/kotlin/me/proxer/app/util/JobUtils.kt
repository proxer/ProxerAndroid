package me.proxer.app.util

import com.evernote.android.job.Job

object JobUtils {

    fun shouldShowError(params: Job.Params, error: Throwable) = when {
        ErrorUtils.isIpBlockedError(error) -> true
        params.failureCount >= 1 && !ErrorUtils.isNetworkError(error) -> true
        else -> false
    }
}
