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

        private fun schedule(context: Context) {
            val interval = PreferenceHelper.getNotificationsInterval(context) * 1000 * 60

            JobRequest.Builder(TAG)
                    .setPeriodic(interval)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setUpdateCurrent(true)
                    .setPersisted(true)
                    .build()
                    .schedule()
        }
    }

    override fun onRunJob(params: Params?): Result {
        try {
            fetchNews(context)
        } catch (error: Throwable) {
            NotificationHelper.showNewsErrorNotification(context, error)

            return Result.FAILURE
        }

        // TODO: Implement new general notifications API.

        return Result.SUCCESS
    }

    private fun fetchNews(context: Context) {
        if (!NewsArticleFragment.isActive) {
            val lastNewsDate = StorageHelper.lastNewsDate
            val newNews = api.notifications().news()
                    .page(0)
                    .limit(100)
                    .build()
                    .execute()
                    .filter { it.date.after(lastNewsDate) }
                    .sortedBy { it.date }

            newNews.firstOrNull()?.date?.let {
                StorageHelper.lastNewsDate = it
            }

            NotificationHelper.showOrUpdateNewsNotification(context, newNews)
        }
    }
}