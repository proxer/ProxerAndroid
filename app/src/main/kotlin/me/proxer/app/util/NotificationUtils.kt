package me.proxer.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import me.proxer.app.R
import me.proxer.app.util.extension.notificationManager

/**
 * @author Ruben Gees
 */
object NotificationUtils {

    const val NEWS_CHANNEL = "proxer_news"
    const val PROFILE_CHANNEL = "proxer_profile"
    const val CHAT_CHANNEL = "proxer_chat"
    const val MANGA_CHANNEL = "proxer_manga"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val newsTitle = context.getString(R.string.notification_channel_news)
            val profileTitle = context.getString(R.string.notification_channel_profile)
            val chatTitle = context.getString(R.string.notification_channel_chat)
            val mangaTitle = context.getString(R.string.notification_channel_manga)

            context.notificationManager.createNotificationChannels(listOf(
                    NotificationChannel(NEWS_CHANNEL, newsTitle, NotificationManager.IMPORTANCE_LOW).apply {
                        description = context.getString(R.string.notification_channel_news_description)
                    },
                    NotificationChannel(PROFILE_CHANNEL, profileTitle, NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = context.getString(R.string.notification_channel_profile_description)
                    },
                    NotificationChannel(CHAT_CHANNEL, chatTitle, NotificationManager.IMPORTANCE_HIGH).apply {
                        description = context.getString(R.string.notification_channel_chat_description)
                    },
                    NotificationChannel(MANGA_CHANNEL, mangaTitle, NotificationManager.IMPORTANCE_LOW).apply {
                        description = context.getString(R.string.notification_channel_manga_description)
                    }
            ))
        }
    }

    fun showErrorNotification(context: Context, id: Int, channel: String, title: String, content: String,
                              intent: PendingIntent? = null) {
        NotificationManagerCompat.from(context).notify(id, NotificationCompat.Builder(context, channel)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(content))
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build())
    }
}
