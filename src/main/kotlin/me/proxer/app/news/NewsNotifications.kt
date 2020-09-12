package me.proxer.app.news

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.forum.TopicActivity
import me.proxer.app.util.NotificationUtils.NEWS_CHANNEL
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.extension.getQuantityString
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toInstantBP
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.DrawerItem
import me.proxer.library.entity.notifications.NewsArticle
import org.koin.core.KoinComponent

/**
 * @author Ruben Gees
 */
object NewsNotifications : KoinComponent {

    private const val ID = 1_357_913_213

    private val preferenceHelper by safeInject<PreferenceHelper>()

    fun showOrUpdate(context: Context, news: Collection<NewsArticle>) {
        when (val notification = buildNewsNotification(context, news)) {
            null -> NotificationManagerCompat.from(context).cancel(ID)
            else -> NotificationManagerCompat.from(context).notify(ID, notification)
        }
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(ID)

    private fun buildNewsNotification(context: Context, news: Collection<NewsArticle>): Notification? {
        if (news.isEmpty()) {
            return null
        }

        val builder = NotificationCompat.Builder(context, NEWS_CHANNEL)
        val newsAmount = context.getQuantityString(R.plurals.notification_news_amount, news.size)
        val style: NotificationCompat.Style
        val intent: PendingIntent
        val title: String
        val content: String

        when (news.size) {
            1 -> {
                val current = news.first()

                title = current.subject.trim()
                content = current.description.trim()
                intent = PendingIntent.getActivity(
                    context,
                    ID,
                    TopicActivity.getIntent(context, current.threadId, current.categoryId, current.subject),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                style = NotificationCompat.BigTextStyle(builder)
                    .bigText(content)
                    .setBigContentTitle(title)
                    .setSummaryText(newsAmount)
            }
            else -> {
                title = context.getString(R.string.notification_news_title)
                content = newsAmount
                intent = PendingIntent.getActivity(
                    context,
                    ID,
                    MainActivity.getSectionIntent(context, DrawerItem.NEWS),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                style = NotificationCompat.InboxStyle().also {
                    news.forEach { newsArticle ->
                        it.addLine(newsArticle.subject)
                    }

                    it.setBigContentTitle(context.getString(R.string.notification_news_title))
                    it.setSummaryText(newsAmount)
                }
            }
        }

        val shouldAlert = news
            .maxByOrNull { it.date }
            ?.date?.toInstantBP()
            ?.isAfter(preferenceHelper.lastNewsDate)
            ?: true

        return builder.setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_stat_proxer)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(intent)
            .addAction(
                R.drawable.ic_stat_check,
                context.getString(R.string.notification_news_read_action),
                NewsNotificationReadReceiver.getPendingIntent(context)
            )
            .setDefaults(if (shouldAlert) Notification.DEFAULT_ALL else 0)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setNumber(news.size)
            .setStyle(style)
            .build()
    }
}
