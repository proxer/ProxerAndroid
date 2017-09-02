package me.proxer.app.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.NotificationUtils.PROFILE_CHANNEL
import me.proxer.app.util.compat.HtmlCompat
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.getQuantityString

/**
 * @author Ruben Gees
 */
object AccountNotifications {

    private const val ID = 759234852

    fun showOrUpdate(context: Context, notifications: Collection<ProxerNotification>) {
        val notification = buildNotification(context, notifications)

        when (notification) {
            null -> NotificationManagerCompat.from(context).cancel(ID)
            else -> NotificationManagerCompat.from(context).notify(ID, notification)
        }
    }

    fun showError(context: Context, error: Throwable) {
        NotificationUtils.showErrorNotification(context, ID, PROFILE_CHANNEL,
                context.getString(R.string.notification_account_error_title),
                context.getString(ErrorUtils.getMessage(error)))
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(ID)

    private fun buildNotification(context: Context, notifications: Collection<ProxerNotification>): Notification? {
        if (notifications.isEmpty()) {
            return null
        }

        val builder = NotificationCompat.Builder(context, PROFILE_CHANNEL)
        val notificationAmount = context.getQuantityString(R.plurals.notification_account_amount, notifications.size)
        val title = context.getString(R.string.notification_account_title)
        val style: NotificationCompat.Style
        val intent: PendingIntent
        val content: CharSequence

        when (notifications.size) {
            1 -> {
                content = HtmlCompat.fromHtml(notifications.first().text)

                intent = PendingIntent.getActivity(context, 0,
                        Intent(Intent.ACTION_VIEW, notifications.first().contentLink.androidUri()),
                        PendingIntent.FLAG_UPDATE_CURRENT)

                style = NotificationCompat.BigTextStyle(builder)
                        .bigText(content)
                        .setBigContentTitle(title)
                        .setSummaryText(notificationAmount)
            }
            else -> {
                content = notificationAmount

                intent = PendingIntent.getActivity(context, 0,
                        NotificationActivity.getIntent(context),
                        PendingIntent.FLAG_UPDATE_CURRENT)

                style = NotificationCompat.InboxStyle().apply {
                    notifications.forEach {
                        addLine(HtmlCompat.fromHtml(it.text))
                    }

                    setBigContentTitle(title)
                    setSummaryText(notificationAmount)
                }
            }
        }

        return builder.setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(intent)
                .setDeleteIntent(AccountNotificationDeletionReceiver.getPendingIntent(context))
                .addAction(R.drawable.ic_stat_check, context.getString(R.string.notification_news_read_action),
                        AccountNotificationReadReceiver.getPendingIntent(context))
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setNumber(notifications.size)
                .setOnlyAlertOnce(true)
                .setStyle(style)
                .build()
    }
}
