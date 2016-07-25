package com.proxerme.app.helper

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.support.annotation.IntDef
import android.support.v4.app.NotificationCompat.*
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import android.support.v7.app.NotificationCompat
import com.proxerme.app.R
import com.proxerme.app.activity.ChatActivity
import com.proxerme.app.activity.DashboardActivity
import com.proxerme.app.receiver.BootReceiver
import com.proxerme.app.receiver.NotificationReceiver
import com.proxerme.app.service.NotificationService
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.experimental.chat.entity.Conference
import com.proxerme.library.connection.notifications.entitiy.News
import com.proxerme.library.info.ProxerUrlHolder


/**
 * A helper class for displaying notifications.

 * @author Ruben Gees
 */
object NotificationHelper {

    const val NEWS_NOTIFICATION = 1423L
    const val CHAT_NOTIFICATION = 1424L

    fun isNewsRetrievalEnabled(context: Context): Boolean {
        return PreferenceHelper.areNewsNotificationsEnabled(context)
    }

    fun retrieveNewsLater(context: Context) {
        cancelNewsRetrieval(context)
        if (isNewsRetrievalEnabled(context)) {
            val interval = PreferenceHelper.getNewsUpdateInterval(context) * 60L * 1000L

            retrieveLater(context, NotificationService.ACTION_LOAD_NEWS, interval)
        }
    }

    fun cancelNewsRetrieval(context: Context) {
        cancelRetrieval(context, NotificationService.ACTION_LOAD_NEWS)
    }

    fun isChatRetrievalEnabled(context: Context): Boolean {
        return PreferenceHelper.areChatNotificationsEnabled(context)
    }

    fun retrieveChatLater(context: Context) {
        cancelChatRetrieval(context)
        if (isChatRetrievalEnabled(context)) {
            val interval = StorageHelper.chatInterval * 1000

            retrieveLater(context, NotificationService.ACTION_LOAD_CHAT, interval)
        }
    }

    fun cancelChatRetrieval(context: Context) {
        cancelRetrieval(context, NotificationService.ACTION_LOAD_CHAT)
    }

    private fun retrieveLater(context: Context,
                              @NotificationService.NotificationAction action: String,
                              interval: Long) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
                .apply { this.action = action }
        val alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + interval, interval, alarmIntent)

        val receiverName = ComponentName(context, BootReceiver::class.java)
        val pm = context.packageManager

        pm.setComponentEnabledSetting(receiverName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)
    }

    private fun cancelRetrieval(context: Context,
                                @NotificationService.NotificationAction action: String) {

        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .cancel(PendingIntent.getBroadcast(context, 0, Intent(context,
                        NotificationReceiver::class.java).apply { this.action = action }, 0))
        val receiverName = ComponentName(context, BootReceiver::class.java)
        val pm = context.packageManager

        if (getOngoingRetrievals(context) > 0) {
            pm.setComponentEnabledSetting(receiverName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
        }
    }

    private fun getOngoingRetrievals(context: Context): Int {
        var count = 0

        if (isNewsRetrievalEnabled(context)) {
            count++
        }

        if (isChatRetrievalEnabled(context)) {
            count++
        }

        return count
    }

    fun showNewsNotification(context: Context, news: List<News>) {
        if (news.size <= 0) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        val builder = NotificationCompat.Builder(context)
        val style: Style
        val amount = context.resources.getQuantityString(
                R.plurals.notification_news_amount, news.size, news.size)

        if (news.size == 1) {
            val current = news[0]
            val title = current.subject.trim()
            val content = current.description.trim()

            builder.setContentTitle(title)
            builder.setContentText(content)

            style = BigTextStyle(builder)
                    .bigText(content)
                    .setBigContentTitle(title)
                    .setSummaryText(amount)
        } else {
            val inboxStyle = InboxStyle()

            news.take(5).forEach {
                inboxStyle.addLine(it.subject)
            }

            inboxStyle.setBigContentTitle(context.getString(R.string.notification_news_title))
                    .setSummaryText(amount)

            style = inboxStyle
        }

        builder.setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(context.getString(R.string.notification_news_title))
                .setContentText(amount)
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        DashboardActivity.getSectionIntent(context, MaterialDrawerHelper.ITEM_NEWS),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setStyle(style)

        notificationManager.notify(NEWS_NOTIFICATION.toInt(), builder.build())
    }

    fun showChatNotification(context: Context,
                             conferences: Collection<Conference>) {
        if (conferences.size < 0) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        val builder = NotificationCompat.Builder(context)
        val inboxStyle = InboxStyle()
        val amount = context.resources.getQuantityString(R.plurals.notification_chat_amount,
                conferences.size, conferences.size)
        val intent: PendingIntent

        inboxStyle.setBigContentTitle(context.getString(R.string.notification_chat_title))
                .setSummaryText(amount)

        conferences.take(5).forEach {
            inboxStyle.addLine(it.topic)
        }

        if (conferences.size == 1) {
            val conference = conferences.first()
            val stackBuilder = TaskStackBuilder.create(context)

            stackBuilder.addNextIntentWithParentStack(ChatActivity.getIntent(context,
                    conference))

            intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

            builder.setLargeIcon(Utils.getBitmapFromURL(context,
                    ProxerUrlHolder.getUserImageUrl(conference.imageId)))
        } else {
            intent = PendingIntent.getActivity(
                    context, 0, DashboardActivity.getSectionIntent(context,
                    MaterialDrawerHelper.ITEM_CHAT), PendingIntent.FLAG_UPDATE_CURRENT)
        }

        builder.setContentTitle(context.getString(R.string.notification_chat_title))
                .setContentText(amount)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setDefaults(Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND or
                        Notification.DEFAULT_LIGHTS)
                .setContentIntent(intent)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setStyle(inboxStyle)
                .setAutoCancel(true)

        notificationManager.notify(CHAT_NOTIFICATION.toInt(), builder.build())
    }

    fun cancelNotification(context: Context, @NotificationId id: Long) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .cancel(id.toInt())
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(NEWS_NOTIFICATION, CHAT_NOTIFICATION)
    annotation class NotificationId
}
