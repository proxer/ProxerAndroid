package me.proxer.app.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import com.rubengees.rxbus.RxBus
import me.proxer.app.news.NewsNotificationEvent
import me.proxer.app.news.NewsNotifications
import me.proxer.app.util.WorkerUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.ProxerApi
import me.proxer.library.api.ProxerCall
import me.proxer.library.entity.notifications.NotificationInfo
import me.proxer.library.enums.NotificationFilter
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class NotificationWorker : Worker(), KoinComponent {

    companion object : KoinComponent {
        private const val NAME = "NotificationWorker"

        private val bus by inject<RxBus>()

        fun enqueueIfPossible(context: Context) {
            val areNotificationsEnabled = PreferenceHelper.areNewsNotificationsEnabled(context) ||
                PreferenceHelper.areAccountNotificationsEnabled(context)

            if (areNotificationsEnabled) {
                enqueue(context)
            } else {
                cancel()
            }
        }

        fun cancel() {
            WorkManager.getInstance().cancelUniqueWork(NAME)
        }

        private fun enqueue(context: Context) {
            val interval = PreferenceHelper.getNotificationsInterval(context) * 1000 * 60
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(interval, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance().enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.REPLACE, workRequest)
        }
    }

    private val api by inject<ProxerApi>()

    private var currentCall: ProxerCall<*>? = null

    override fun onStopped(cancelled: Boolean) {
        currentCall?.cancel()
    }

    override fun doWork() = try {
        val notificationInfo = when (StorageHelper.isLoggedIn) {
            true -> api.notifications().notificationInfo()
                .build()
                .also { currentCall = it }
                .execute()
            false -> null
        }

        val areNewsNotificationsEnabled = PreferenceHelper.areNewsNotificationsEnabled(applicationContext)
        val areAccountNotificationsEnabled = PreferenceHelper.areAccountNotificationsEnabled(applicationContext)

        if (!isStopped && areNewsNotificationsEnabled) {
            fetchNews(applicationContext, notificationInfo)
        }

        if (!isStopped && areAccountNotificationsEnabled && notificationInfo != null) {
            fetchAccountNotifications(applicationContext, notificationInfo)
        }

        Result.SUCCESS
    } catch (error: Throwable) {
        if (!isStopped && WorkerUtils.shouldShowError(runAttemptCount, error)) {
            AccountNotifications.showError(applicationContext, error)

            Result.FAILURE
        } else {
            Result.RETRY
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
                .also { currentCall = it }
                .safeExecute()
                .asSequence()
                .filter { it.date.after(lastNewsDate) }
                .sortedByDescending { it.date }
                .toList()
        }

        newNews.firstOrNull()?.date?.let {
            if (!isStopped && it != lastNewsDate && !bus.post(NewsNotificationEvent())) {
                NewsNotifications.showOrUpdate(context, newNews)

                StorageHelper.lastNewsDate = it
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
                .also { currentCall = it }
                .safeExecute()
        }

        newNotifications.firstOrNull()?.date?.let {
            if (!isStopped && it != lastNotificationsDate && !bus.post(AccountNotificationEvent())) {
                AccountNotifications.showOrUpdate(context, newNotifications)

                StorageHelper.lastNotificationsDate = it
            }
        }
    }
}
