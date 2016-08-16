package com.proxerme.app.helper

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Typeface
import android.support.annotation.IntDef
import android.support.v4.app.NotificationCompat.*
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import android.support.v7.app.NotificationCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.activity.ChatActivity
import com.proxerme.app.activity.DashboardActivity
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.notifications.entitiy.News
import com.proxerme.library.info.ProxerUrlHolder
import org.jetbrains.anko.notificationManager


/**
 * A helper class for displaying notifications.

 * @author Ruben Gees
 */
object NotificationHelper {

    const val NEWS_NOTIFICATION = 1423L
    const val CHAT_NOTIFICATION = 1424L

    fun showNewsNotification(context: Context, news: Collection<News>) {
        if (news.size <= 0) {
            context.notificationManager.cancel(NEWS_NOTIFICATION.toInt())

            return
        }

        val builder = NotificationCompat.Builder(context)
        val style: Style
        val amount = context.resources.getQuantityString(
                R.plurals.notification_news_amount, news.size, news.size)

        if (news.size == 1) {
            val current = news.first()
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

            news.forEach {
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
                .setPriority(PRIORITY_LOW)
                .setStyle(style)

        context.notificationManager.notify(NEWS_NOTIFICATION.toInt(), builder.build())
    }

    fun showChatNotification(context: Context,
                             messages: Map<LocalConference, List<LocalMessage>>) {
        if (messages.size <= 0) {
            context.notificationManager.cancel(CHAT_NOTIFICATION.toInt())

            return
        }

        val builder = NotificationCompat.Builder(context)
        val inboxStyle = InboxStyle()
        var messageAmount = 0

        messages.forEach { entry ->
            messageAmount += entry.value.size

            entry.value.forEach { message ->
                val sender = if (entry.key.isGroup) message.username + "@" +
                        entry.key.topic else entry.key.topic

                inboxStyle.addLine(SpannableString(sender + " " + message.message).apply {
                    this.setSpan(StyleSpan(Typeface.BOLD), 0, sender.length,
                            Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                })
            }
        }

        if (messages.size == 1) {
            inboxStyle.setBigContentTitle(messages.keys.first().topic)
                    .setSummaryText(context.resources
                            .getQuantityString(R.plurals.notification_chat_message_amount,
                                    messageAmount, messageAmount))

            builder.setContentTitle(messages.keys.first().topic)
            builder.setContentIntent(TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(ChatActivity
                            .getIntent(context, messages.keys.first()))
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))

            if (messages.values.first().size == 1) {
                builder.setContentText(messages.values.first().first().message)
            } else {
                builder.setContentText(context.resources
                        .getQuantityString(R.plurals.notification_chat_message_amount,
                                messageAmount, messageAmount))
            }

            if (messages.keys.first().imageId.isNotBlank()) {
                builder.setLargeIcon(Utils.getBitmapFromURL(context,
                        ProxerUrlHolder.getUserImageUrl(messages.keys.first().imageId)))
            } else {
                if (messages.keys.first().isGroup) {
                    builder.setLargeIcon(
                            IconicsDrawable(context, CommunityMaterial.Icon.cmd_account_multiple)
                                    .sizeDp(96)
                                    .colorRes(R.color.colorPrimary)
                                    .toBitmap())
                } else {
                    builder.setLargeIcon(
                            IconicsDrawable(context, CommunityMaterial.Icon.cmd_account)
                                    .sizeDp(96)
                                    .colorRes(R.color.colorPrimary)
                                    .toBitmap())
                }
            }
        } else {
            val title = context.resources
                    .getQuantityString(R.plurals.notification_chat_message_amount,
                            messageAmount, messageAmount) + " " +
                    context.resources
                            .getQuantityString(R.plurals.notification_chat_conference_amount,
                                    messages.size, messages.size)

            inboxStyle.setBigContentTitle(title)
            builder.setContentTitle(title)
            builder.setContentText(messages.keys.joinToString(", ", transform = { it.topic }))

            builder.setContentIntent(PendingIntent.getActivity(context, 0, DashboardActivity
                    .getSectionIntent(context, MaterialDrawerHelper.ITEM_CHAT),
                    PendingIntent.FLAG_UPDATE_CURRENT))
        }

        builder.setSmallIcon(R.drawable.ic_stat_proxer)
                .setDefaults(Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND or
                        Notification.DEFAULT_LIGHTS)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(PRIORITY_HIGH)
                .setCategory(CATEGORY_MESSAGE)
                .setStyle(inboxStyle)
                .setAutoCancel(true)

        context.notificationManager.notify(CHAT_NOTIFICATION.toInt(), builder.build())
    }

    fun cancelNotification(context: Context, @NotificationId id: Long) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .cancel(id.toInt())
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(NEWS_NOTIFICATION, CHAT_NOTIFICATION)
    annotation class NotificationId
}
