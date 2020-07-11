package me.proxer.app.notification

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rubengees.rxbus.RxBus
import me.proxer.app.news.NewsNotificationEvent
import me.proxer.app.news.NewsNotifications
import me.proxer.app.util.WorkerUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toInstantBP
import me.proxer.library.ProxerApi
import me.proxer.library.ProxerCall
import me.proxer.library.entity.notifications.NotificationInfo
import me.proxer.library.enums.NotificationFilter
import org.koin.core.KoinComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams), KoinComponent {

    companion object : KoinComponent {
        private const val NAME = "NotificationWorker"

        private val bus by safeInject<RxBus>()
        private val workManager by safeInject<WorkManager>()
        private val preferenceHelper by safeInject<PreferenceHelper>()

        fun enqueueIfPossible(delay: Boolean = false) {
            val areNotificationsEnabled = preferenceHelper.areNewsNotificationsEnabled ||
                preferenceHelper.areAccountNotificationsEnabled

            if (areNotificationsEnabled) {
                enqueue(delay)
            } else {
                cancel()
            }
        }

        fun cancel() {
            workManager.cancelUniqueWork(NAME)
        }

        private fun enqueue(delay: Boolean) {
            val interval = preferenceHelper.notificationsInterval * 1_000 * 60
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(interval, TimeUnit.MILLISECONDS)
                .apply { if (delay) setInitialDelay(interval, TimeUnit.MILLISECONDS) }
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.REPLACE, workRequest)
        }
    }

    private val api by safeInject<ProxerApi>()
    private val storageHelper by safeInject<StorageHelper>()

    private var currentCall: ProxerCall<*>? = null

    override fun onStopped() {
        currentCall?.cancel()
    }

    override fun doWork() = try {
        val notificationInfo = when (storageHelper.isLoggedIn) {
            true -> api.notifications.notificationInfo()
                .build()
                .also { currentCall = it }
                .execute()
            false -> null
        }

        val areNewsNotificationsEnabled = preferenceHelper.areNewsNotificationsEnabled
        val areAccountNotificationsEnabled = preferenceHelper.areAccountNotificationsEnabled

        if (!isStopped && areNewsNotificationsEnabled) {
            fetchNews(applicationContext, notificationInfo)
        }

        if (!isStopped && areAccountNotificationsEnabled && notificationInfo != null) {
            fetchAccountNotifications(applicationContext, notificationInfo)
        }

        Result.success()
    } catch (error: Throwable) {
        Timber.e(error)

        if (WorkerUtils.shouldRetryForError(error)) {
            Result.retry()
        } else {
            Result.failure()
        }
    }

    private fun fetchNews(context: Context, notificationInfo: NotificationInfo?) {
        val lastNewsDate = preferenceHelper.lastNewsDate
        val newNews = when (notificationInfo?.newsAmount) {
            0 -> emptyList()
            else -> api.notifications.news()
                .page(0)
                .limit(notificationInfo?.newsAmount ?: 100)
                .build()
                .also { currentCall = it }
                .safeExecute()
                .asSequence()
                .filter { it.date.toInstantBP().isAfter(lastNewsDate) }
                .sortedByDescending { it.date }
                .toList()
        }

        newNews.firstOrNull()?.date?.toInstantBP()?.let {
            if (!isStopped && it != lastNewsDate && !bus.post(NewsNotificationEvent())) {
                NewsNotifications.showOrUpdate(context, newNews)

                preferenceHelper.lastNewsDate = it
            }
        }
    }

    private fun fetchAccountNotifications(context: Context, notificationInfo: NotificationInfo) {
        val lastNotificationsDate = storageHelper.lastNotificationsDate
        val newNotifications = when (notificationInfo.notificationAmount) {
            0 -> emptyList()
            else -> api.notifications.notifications()
                .page(0)
                .limit(notificationInfo.notificationAmount)
                .filter(NotificationFilter.UNREAD)
                .build()
                .also { currentCall = it }
                .safeExecute()
        }

        newNotifications.firstOrNull()?.date?.toInstantBP()?.let {
            if (!isStopped && it != lastNotificationsDate && !bus.post(AccountNotificationEvent())) {
                AccountNotifications.showOrUpdate(context, newNotifications)

                storageHelper.lastNotificationsDate = it
            }
        }
    }
}
