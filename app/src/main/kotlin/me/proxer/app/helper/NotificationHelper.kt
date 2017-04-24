package me.proxer.app.helper

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.support.v4.app.NotificationCompat.*
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.NotificationCompat
import me.proxer.app.R
import me.proxer.app.activity.DashboardActivity
import me.proxer.app.fragment.news.NewsArticleFragment
import me.proxer.app.helper.MaterialDrawerHelper.DrawerItem
import me.proxer.app.util.ErrorUtils
import me.proxer.library.api.ProxerException
import me.proxer.library.entitiy.notifications.NewsArticle

/**
 * @author Ruben Gees
 */
object NotificationHelper {

    fun showOrUpdateNewsNotification(context: Context, news: Collection<NewsArticle>) {
        if (!NewsArticleFragment.isActive) {
            val notification = buildNewsNotification(context, news)

            when (notification) {
                null -> cancelNotification(context, NotificationType.NEWS)
                else -> NotificationManagerCompat.from(context).notify(NotificationType.NEWS.id, notification)
            }
        }
    }

    fun showMangaDownloadErrorNotification(context: Context, error: Throwable) {
        val message = context.getString(when (error) {
            is ProxerException -> ErrorUtils.getMessageForProxerException(error)
            else -> R.string.notification_manga_download_error_content
        })

        NotificationManagerCompat.from(context)
                .notify(NotificationType.MANGA_DOWNLOAD_ERROR.id, NotificationCompat.Builder(context)
                        .setContentTitle(context.getString(R.string.notification_manga_download_error_title))
                        .setContentText(message)
                        .setColor(ContextCompat.getColor(context, R.color.primary))
                        .setSmallIcon(R.drawable.ic_stat_proxer)
                        .setPriority(PRIORITY_LOW)
                        .setAutoCancel(true)
                        .build())
    }

    fun cancelNotification(context: Context, type: NotificationType) {
        NotificationManagerCompat.from(context).cancel(type.id)
    }

    private fun buildNewsNotification(context: Context, news: Collection<NewsArticle>): Notification? {
        if (news.isEmpty()) {
            return null
        }

        val builder = NotificationCompat.Builder(context)
        val newsAmount = context.resources.getQuantityString(R.plurals.notification_news_amount, news.size, news.size)
        val style: Style
        val title: String
        val content: String

        when (news.size) {
            1 -> {
                val current = news.first()

                title = current.subject.trim()
                content = current.description.trim()

                style = BigTextStyle(builder)
                        .bigText(content)
                        .setBigContentTitle(title)
                        .setSummaryText(newsAmount)
            }
            else -> {
                title = context.getString(R.string.notification_news_title)
                content = newsAmount

                style = InboxStyle().apply {
                    news.forEach {
                        addLine(it.subject)
                    }

                    setBigContentTitle(context.getString(R.string.notification_news_title))
                            .setSummaryText(newsAmount)
                }
            }
        }

        return builder.setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        DashboardActivity.getSectionIntent(context, DrawerItem.NEWS),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setStyle(style)
                .build()
    }

    enum class NotificationType(val id: Int) {
        NEWS(1357913213),
        MANGA_DOWNLOAD_ERROR(479239223)
    }
}
