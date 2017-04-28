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
            fetchNews(context)
        } catch (error: Throwable) {
            NotificationHelper.showNewsErrorNotification(context, error)
        }

        // TODO: Implement new general notifications API.

        return Result.SUCCESS
    }

    private fun fetchNews(context: Context) {
        if (!NewsArticleFragment.isActive) {
            val lastNewsId = StorageHelper.lastNewsId
            val newNews = api.notifications().news()
                    .page(0)
                    .limit(15)
                    .build()
                    .execute()
                    .filter { it.id.toLong() > lastNewsId.toLong() }

            newNews.firstOrNull()?.id?.let {
                StorageHelper.lastNewsId = it
            }

            NotificationHelper.showOrUpdateNewsNotification(context, newNews)
        }
    }
}