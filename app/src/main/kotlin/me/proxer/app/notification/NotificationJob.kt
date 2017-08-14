package me.proxer.app.notification

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.news.NewsNotificationEvent
import me.proxer.app.news.NewsNotifications
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper

/**
 * @author Ruben Gees
 */
class NotificationJob : Job() {

    companion object {
        const val TAG = "notification_job"

        fun scheduleIfPossible(context: Context) {
            val areNotificationsEnabled = PreferenceHelper.areNewsNotificationsEnabled(context) ||
                    PreferenceHelper.areAccountNotificationsEnabled(context)

            if (areNotificationsEnabled) {
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
                    .build()
                    .schedule()
        }
    }

    override fun onRunJob(params: Params?): Result {
        if (PreferenceHelper.areNewsNotificationsEnabled(context)) {
            try {
                fetchNews(context)
            } catch (error: Throwable) {
                NewsNotifications.showError(context, error)

                return Result.FAILURE
            }
        }

//        if (PreferenceHelper.areAccountNotificationsEnabled(context)) {
//            try {
//                fetchAccountNotifications(context)
//            } catch (error: Throwable) {
//                AccountNotificationHelper.showError(context, error)
//
//                return Result.FAILURE
//            }
//        }

        return Result.SUCCESS
    }

    private fun fetchNews(context: Context) {
        val lastNewsDate = StorageHelper.lastNewsDate
        val newNews = api.notifications().news()
                .page(0)
                .limit(100)
                .build()
                .execute()
                .filter { it.date.after(lastNewsDate) }
                .sortedByDescending { it.date }

        newNews.firstOrNull()?.date?.let {
            StorageHelper.lastNewsDate = it
        }

        if (!bus.post(NewsNotificationEvent(newNews))) {
            NewsNotifications.showOrUpdate(context, newNews)
        }
    }

//    private fun fetchAccountNotifications(context: Context) {
//        val user = StorageHelper.user
//
//        if (user != null) {
//            val notificationInfo = api.notifications().notificationInfo().build().execute()
//            val newNotifications = when (notificationInfo.notifications == 0) {
//                true -> emptyList()
//                false -> api.notifications().notifications()
//                        .limit(notificationInfo.notifications)
//                        .markAsRead(true)
//                        .build()
//                        .execute()
//            }
//
//            AccountNotificationHelper.showOrUpdate(context, newNotifications)
//        } else {
//            AccountNotificationHelper.cancel(context)
//        }
//    }
}
