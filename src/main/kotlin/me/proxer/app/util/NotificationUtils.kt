package me.proxer.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import me.proxer.app.R

/**
 * @author Ruben Gees
 */
object NotificationUtils {

    const val NEWS_CHANNEL = "proxer_news"
    const val PROFILE_CHANNEL = "proxer_profile"
    const val CHAT_CHANNEL = "proxer_chat"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val newsTitle = context.getString(R.string.notification_channel_news)
            val profileTitle = context.getString(R.string.notification_channel_profile)
            val chatTitle = context.getString(R.string.notification_channel_chat)

            context.getSystemService<NotificationManager>()?.createNotificationChannels(
                listOf(
                    NotificationChannel(NEWS_CHANNEL, newsTitle, NotificationManager.IMPORTANCE_LOW).apply {
                        description = context.getString(R.string.notification_channel_news_description)
                    },
                    NotificationChannel(PROFILE_CHANNEL, profileTitle, NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = context.getString(R.string.notification_channel_profile_description)
                    },
                    NotificationChannel(CHAT_CHANNEL, chatTitle, NotificationManager.IMPORTANCE_HIGH).apply {
                        description = context.getString(R.string.notification_channel_chat_description)
                    }
                )
            )
        }
    }
}
