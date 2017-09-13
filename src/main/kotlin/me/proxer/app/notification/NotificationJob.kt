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
import me.proxer.library.entity.notifications.NotificationInfo
import me.proxer.library.enums.NotificationFilter

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

    override fun onRunJob(params: Params) = try {
        val notificationInfo = when (StorageHelper.user != null) {
            true -> api.notifications().notificationInfo().build().execute()
            false -> null
        }

        if (PreferenceHelper.areNewsNotificationsEnabled(context)) {
            fetchNews(context, notificationInfo)
        }

        if (PreferenceHelper.areAccountNotificationsEnabled(context) && notificationInfo != null) {
            fetchAccountNotifications(context, notificationInfo)
        }

        Result.SUCCESS
    } catch (error: Throwable) {
        if (params.failureCount >= 1) {
            AccountNotifications.showError(context, error)

            Result.FAILURE
        } else {
            Result.RESCHEDULE
        }
    }

    private fun fetchNews(context: Context, notificationInfo: NotificationInfo?) {
        val lastNewsDate = StorageHelper.lastNewsDate
        val newNews = when (notificationInfo?.news) {
            0 -> emptyList()
            else -> api.notifications().news()
                    .page(0)
                    .limit(notificationInfo?.news ?: 100)
                    .build()
                    .safeExecute()
                    .filter { it.date.after(lastNewsDate) }
                    .sortedByDescending { it.date }
        }

        newNews.firstOrNull()?.date.let {
            if (it != lastNewsDate && !bus.post(NewsNotificationEvent())) {
                NewsNotifications.showOrUpdate(context, newNews)
            }
        }
    }

    private fun fetchAccountNotifications(context: Context, notificationInfo: NotificationInfo) {
        val lastNotificationsDate = StorageHelper.lastNotificationsDate
        val newNotifications = when (notificationInfo.notifications) {
            0 -> emptyList()
            else -> api.notifications().notifications()
                    .page(0)
                    .limit(notificationInfo.notifications)
                    .filter(NotificationFilter.UNREAD)
                    .build()
                    .safeExecute()
        }

        newNotifications.firstOrNull()?.date.let {
            if (it != lastNotificationsDate && !bus.post(AccountNotificationEvent())) {
                AccountNotifications.showOrUpdate(context, newNotifications)
            }
        }
    }
}
