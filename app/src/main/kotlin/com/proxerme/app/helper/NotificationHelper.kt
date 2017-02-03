package com.proxerme.app.helper

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Build
import android.support.annotation.IntDef
import android.support.v4.app.NotificationCompat.*
import android.support.v4.app.RemoteInput
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import android.support.v7.app.NotificationCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.proxerme.app.R
import com.proxerme.app.activity.DashboardActivity
import com.proxerme.app.activity.chat.ChatActivity
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.receiver.DirectReplyReceiver
import com.proxerme.app.util.Utils
import com.proxerme.app.util.notificationManager
import com.proxerme.library.connection.notifications.entitiy.News
import com.proxerme.library.info.ProxerUrlHolder


/**
 * A helper class for displaying notifications.

 * @author Ruben Gees
 */
object NotificationHelper {

    const val NEWS_NOTIFICATION = 13579111543343223L
    const val CHAT_NOTIFICATION = 13579111341234234L

    fun showNewsNotification(context: Context, news: Collection<News>) {
        if (news.isEmpty()) {
            context.notificationManager.cancel(NEWS_NOTIFICATION.toInt())

            return
        }

        val builder = NotificationCompat.Builder(context)
        val newsAmount = context.resources.getQuantityString(
                R.plurals.notification_news_amount, news.size, news.size)
        val style: Style

        when {
            news.size == 1 -> {
                val current = news.first()
                val title = current.subject.trim()
                val content = current.description.trim()

                builder.setContentTitle(title)
                builder.setContentText(content)

                style = BigTextStyle(builder)
                        .bigText(content)
                        .setBigContentTitle(title)
                        .setSummaryText(newsAmount)
            }
            else -> style = InboxStyle().apply {
                news.forEach {
                    addLine(it.subject)
                }

                setBigContentTitle(context.getString(R.string.notification_news_title))
                        .setSummaryText(newsAmount)
            }
        }

        builder.setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(context.getString(R.string.notification_news_title))
                .setContentText(newsAmount)
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
        if (messages.isEmpty()) {
            context.notificationManager.cancel(CHAT_NOTIFICATION.toInt())

            return
        }

        val messageAmount = messages.values.sumBy { it.size }
        val title = buildTitle(context, messages, messageAmount)
        val notificationBuilder = NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(title)
                .setContentText(buildText(context, messages, messageAmount))
                .setLargeIcon(buildIconIfAppropriate(context, messages))
                .setContentIntent(buildIntent(context, messages))
                .setStyle(buildStyle(context, messages, title, messageAmount))
                .setDefaults(Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND or
                        Notification.DEFAULT_LIGHTS)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(PRIORITY_HIGH)
                .setCategory(CATEGORY_MESSAGE)
                .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && messages.size == 1) {
            val remoteInput = RemoteInput.Builder(DirectReplyReceiver.EXTRA_REMOTE_REPLY)
                    .setLabel("Antworten").build()
            val replyIntent = PendingIntent.getBroadcast(context, messages.keys.first().id.toInt(),
                    DirectReplyReceiver.getMessageReplyIntent(messages.keys.first().id),
                    PendingIntent.FLAG_UPDATE_CURRENT)
            val actionReplyByRemoteInput =
                    Action.Builder(R.mipmap.ic_launcher, "Antworten", replyIntent)
                            .addRemoteInput(remoteInput)
                            .setAllowGeneratedReplies(true)
                            .build()

            notificationBuilder.addAction(actionReplyByRemoteInput)
        }

        context.notificationManager.notify(CHAT_NOTIFICATION.toInt(), notificationBuilder.build())
    }

    fun cancelNotification(context: Context, @NotificationId id: Long) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .cancel(id.toInt())
    }

    private fun buildTitle(context: Context, messages: Map<LocalConference, List<LocalMessage>>,
                           messageAmount: Int): String {
        val resources = context.resources

        if (messages.size == 1) {
            return messages.keys.first().topic
        } else {
            return resources.getQuantityString(R.plurals.notification_chat_message_amount,
                    messageAmount, messageAmount) + " " +
                    resources.getQuantityString(R.plurals.notification_chat_conference_amount,
                            messages.size, messages.size)
        }
    }

    private fun buildText(context: Context, messages: Map<LocalConference, List<LocalMessage>>,
                          messageAmount: Int): SpannableString {
        val resources = context.resources

        if (messages.size == 1) {
            if (messages.values.first().size == 1) {
                return buildMessage(messages.keys.first(), messages.values.first().first(), false)
            } else {
                return SpannableString(resources
                        .getQuantityString(R.plurals.notification_chat_message_amount,
                                messageAmount, messageAmount))
            }
        } else {
            return SpannableString(messages.keys.joinToString(", ", transform = { it.topic }))
        }
    }

    private fun buildStyle(context: Context, messages: Map<LocalConference, List<LocalMessage>>,
                           title: String, messageAmount: Int): Style {
        val resources = context.resources

        if (messages.size == 1) {
            val summary = resources.getQuantityString(R.plurals.notification_chat_message_amount,
                    messageAmount, messageAmount)

            if (messages.values.first().size == 1) {
                return BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(buildMessage(messages.keys.first(),
                                messages.values.first().first(), false))
                        .setSummaryText(summary)
            } else {
                return InboxStyle()
                        .setBigContentTitle(title)
                        .setSummaryText(summary)
                        .apply {
                            messages.forEach { entry ->
                                entry.value.forEach {
                                    addLine(buildMessage(entry.key, it, false))
                                }
                            }
                        }
            }
        } else {
            return InboxStyle()
                    .setBigContentTitle(title)
                    .apply {
                        messages.forEach { entry ->
                            entry.value.forEach {
                                addLine(buildMessage(entry.key, it, true))
                            }
                        }
                    }
        }
    }

    private fun buildIntent(context: Context, messages: Map<LocalConference, List<LocalMessage>>):
            PendingIntent {
        if (messages.size == 1) {
            return TaskStackBuilder.create(context)
                    .addNextIntent(DashboardActivity.getSectionIntent(context,
                            MaterialDrawerHelper.ITEM_CHAT))
                    .addNextIntent(ChatActivity.getIntent(context,
                            messages.keys.first()))
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            return PendingIntent.getActivity(context, 0,
                    DashboardActivity.getSectionIntent(context, MaterialDrawerHelper.ITEM_CHAT),
                    PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun buildIconIfAppropriate(context: Context,
                                       messages: Map<LocalConference, List<LocalMessage>>): Bitmap? {
        if (messages.size == 1) {
            if (messages.keys.first().imageId.isNotBlank()) {
                return Utils.getBitmapFromURL(context,
                        ProxerUrlHolder.getUserImageUrl(messages.keys.first().imageId).toString())
            } else {
                val icon: IIcon

                if (messages.keys.first().isGroup) {
                    icon = CommunityMaterial.Icon.cmd_account_multiple
                } else {
                    icon = CommunityMaterial.Icon.cmd_account
                }

                return IconicsDrawable(context, icon)
                        .sizeDp(96)
                        .colorRes(R.color.colorPrimary)
                        .toBitmap()
            }
        } else {
            return null
        }
    }

    private fun buildMessage(conference: LocalConference, message: LocalMessage,
                             insertTopic: Boolean): SpannableString {
        val sender: String

        if (conference.isGroup) {
            if (insertTopic) {
                sender = "${message.username} @ ${conference.topic} "
            } else {
                sender = "${message.username} "
            }
        } else {
            if (insertTopic) {
                sender = "${conference.topic} "
            } else {
                sender = ""
            }
        }

        return SpannableString(sender + message.message).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, sender.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(NEWS_NOTIFICATION, CHAT_NOTIFICATION)
    annotation class NotificationId
}
