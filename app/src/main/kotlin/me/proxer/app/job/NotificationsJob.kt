package me.proxer.app.job

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.news.NewsArticleFragment
import me.proxer.app.helper.NotificationHelper
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper

/**
 * @author Ruben Gees
 */
class NotificationsJob : Job() {

    companion object {
        const val TAG = "notifications_job"

        fun schedule(context: Context) {
            val interval = PreferenceHelper.getNotificationsInterval(context) * 1000 * 60

            JobRequest.Builder(TAG)
                    .setPeriodic(interval)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setUpdateCurrent(true)
                    .setPersisted(true)
                    .build()
                    .schedule()
        }

        fun scheduleIfPossible(context: Context) {
            if (PreferenceHelper.areNotificationsEnabled(context)) {
                schedule(context)
            } else {
                cancel()
            }
        }

        fun cancel() {
            JobManager.instance().cancelAllForTag(TAG)
        }
    }

    override fun onRunJob(params: Params?): Result {
        try {
            if (!NewsArticleFragment.isActive) {
                val lastNewsTime = StorageHelper.lastNewsTime.time
                val newNews = api.notifications().news().build().execute().takeWhile {
                    it.date.time > lastNewsTime
                }

                newNews.firstOrNull()?.date?.let {
                    StorageHelper.lastNewsTime = it
                }

                NotificationHelper.showOrUpdateNewsNotification(context, newNews)
            }

            return Result.SUCCESS
        } catch(error: Throwable) {
            return Result.FAILURE
        }
    }
}