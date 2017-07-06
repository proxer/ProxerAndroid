package me.proxer.app.helper.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import me.proxer.app.R
import me.proxer.app.activity.DashboardActivity
import me.proxer.app.fragment.news.NewsArticleFragment
import me.proxer.app.helper.MaterialDrawerHelper.DrawerItem
import me.proxer.app.helper.notification.NotificationHelper.NEWS_CHANNEL
import me.proxer.app.util.ErrorUtils
import me.proxer.library.entitiy.notifications.NewsArticle

/**
 * @author Ruben Gees
 */
object NewsNotificationHelper {

    private const val ID = 1357913213
    private const val ERROR_ID = 472347289

    fun showOrUpdate(context: Context, news: Collection<NewsArticle>) {
        if (!NewsArticleFragment.isActive) {
            val notification = buildNewsNotification(context, news)

            when (notification) {
                null -> NotificationManagerCompat.from(context).cancel(ID)
                else -> NotificationManagerCompat.from(context).notify(ID, notification)
            }
        }
    }

    fun showError(context: Context, error: Throwable) {
        NotificationHelper.showErrorNotification(context, ID, NEWS_CHANNEL,
                context.getString(R.string.notification_news_error_title),
                context.getString(ErrorUtils.getMessage(error)))
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).let {
            it.cancel(ID)
            it.cancel(ERROR_ID)
        }
    }

    private fun buildNewsNotification(context: Context, news: Collection<NewsArticle>): Notification? {
        if (news.isEmpty()) {
            return null
        }

        val builder = NotificationCompat.Builder(context, NEWS_CHANNEL)
        val newsAmount = context.resources.getQuantityString(R.plurals.notification_news_amount, news.size, news.size)
        val style: NotificationCompat.Style
        val title: String
        val content: String

        when (news.size) {
            1 -> {
                val current = news.first()

                title = current.subject.trim()
                content = current.description.trim()

                style = NotificationCompat.BigTextStyle(builder)
                        .bigText(content)
                        .setBigContentTitle(title)
                        .setSummaryText(newsAmount)
            }
            else -> {
                title = context.getString(R.string.notification_news_title)
                content = newsAmount

                style = NotificationCompat.InboxStyle().apply {
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
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setNumber(news.size)
                .setStyle(style)
                .build()
    }
}