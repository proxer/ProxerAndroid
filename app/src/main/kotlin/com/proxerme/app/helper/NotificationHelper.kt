package com.proxerme.app.helper

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Typeface
import android.os.Build
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
import com.proxerme.app.R
import com.proxerme.app.activity.DashboardActivity
import com.proxerme.app.activity.chat.ChatActivity
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.helper.MaterialDrawerHelper.DrawerItem
import com.proxerme.app.manager.UserManager
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

    private const val GROUP_CHAT = "chat"

    fun showNewsNotification(context: Context, news: Collection<News>) {
        if (news.isEmpty()) {
            context.notificationManager.cancel(NotificationType.NEWS.id)

            return
        }

        context.notificationManager.notify(NotificationType.NEWS.id,
                buildNewsNotification(context, news))
    }

    fun showChatNotification(context: Context, messages: Map<LocalConference, List<LocalMessage>>) {
        if (messages.isEmpty()) {
            context.notificationManager.cancel(NotificationType.CHAT.id)

            return
        }

        messages.entries.map { (conference, messages) ->
            conference.id.toInt() to when {
                messages.isEmpty() -> null
                else -> buildIndividualChatNotification(context, conference, messages)
            }
        }.plus(NotificationType.CHAT.id to buildChatSummaryNotification(context, messages)).forEach {
            when (it.second) {
                null -> context.notificationManager.cancel(it.first)
                else -> context.notificationManager.notify(it.first, it.second)
            }
        }
    }

    fun cancelNotification(context: Context, type: NotificationType) {
        context.notificationManager.cancel(type.id)
    }

    private fun buildNewsNotification(context: Context, news: Collection<News>): Notification {
        val builder = NotificationCompat.Builder(context)
        val newsAmount = context.resources.getQuantityString(R.plurals.notification_news_amount,
                news.size, news.size)
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

        builder.setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        DashboardActivity.getSectionIntent(context, DrawerItem.NEWS),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(PRIORITY_LOW)
                .setStyle(style)

        return builder.build()
    }

    private fun buildChatSummaryNotification(context: Context,
                                             messages: Map<LocalConference, List<LocalMessage>>): Notification {
        val messageAmount = messages.values.sumBy { it.size }
        val title = context.resources.getQuantityString(R.plurals.notification_chat_message_amount,
                messageAmount, messageAmount) +
                " " + context.resources.getQuantityString(R.plurals.notification_chat_conference_amount,
                messages.size, messages.size)
        val content = SpannableString(messages.keys.joinToString(", ", transform = { it.topic }))

        val style = InboxStyle()
                .setBigContentTitle(content)
                .setSummaryText(title)
                .apply {
                    messages.forEach { entry ->
                        entry.value.forEach {
                            val sender = when {
                                entry.key.isGroup -> "${entry.key.topic}: ${it.username} "
                                else -> "${entry.key.topic}: "
                            }

                            addLine(SpannableString(sender + it.message).apply {
                                setSpan(StyleSpan(Typeface.BOLD), 0, sender.length,
                                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                            })
                        }
                    }
                }

        val intent = TaskStackBuilder.create(context)
                .addNextIntent(DashboardActivity.getSectionIntent(context, DrawerItem.CHAT))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(style)
                .setContentIntent(intent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(PRIORITY_HIGH)
                .setCategory(CATEGORY_MESSAGE)
                .setGroup(GROUP_CHAT)
                .setGroupSummary(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .build()
    }

    private fun buildIndividualChatNotification(context: Context, conference: LocalConference,
                                                messages: List<LocalMessage>): Notification {
        val content = context.resources.getQuantityString(R.plurals.notification_chat_message_amount,
                messages.size, messages.size)
        val icon = when {
            conference.imageId.isNotBlank() -> Utils.getBitmapFromURL(context,
                    ProxerUrlHolder.getUserImageUrl(conference.imageId).toString())
            else -> IconicsDrawable(context, when (conference.isGroup) {
                true -> CommunityMaterial.Icon.cmd_account_multiple
                false -> CommunityMaterial.Icon.cmd_account
            }).sizeDp(96).colorRes(R.color.colorPrimary).toBitmap()
        }

        val style = when (conference.isGroup) {
            true -> MessagingStyle(UserManager.user!!.username)
                    .setConversationTitle(conference.topic)
                    .apply {
                        messages.forEach {
                            addMessage(it.message, it.time, it.username)
                        }
                    }
            false -> InboxStyle()
                    .setBigContentTitle(conference.topic)
                    .setSummaryText(content)
                    .apply {
                        messages.forEach {
                            addLine(it.message)
                        }
                    }
        }

        val intent = TaskStackBuilder.create(context)
                .addNextIntent(DashboardActivity.getSectionIntent(context, DrawerItem.CHAT))
                .addNextIntent(ChatActivity.getIntent(context, conference))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(conference.topic)
                .setContentText(content)
                .setLargeIcon(icon)
                .setStyle(style)
                .setContentIntent(intent)
                .setDefaults(Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND or
                        Notification.DEFAULT_LIGHTS)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(PRIORITY_HIGH)
                .setCategory(CATEGORY_MESSAGE)
                .setGroup(GROUP_CHAT)
                .setAutoCancel(true)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val remoteInput = RemoteInput.Builder(DirectReplyReceiver.EXTRA_REMOTE_REPLY)
                                .setLabel(context.getString(R.string.action_answer))
                                .build()

                        val replyIntent = PendingIntent.getBroadcast(context, conference.id.toInt(),
                                DirectReplyReceiver.getMessageReplyIntent(conference.id),
                                PendingIntent.FLAG_UPDATE_CURRENT)

                        val actionReplyByRemoteInput = Action.Builder(R.mipmap.ic_launcher,
                                context.getString(R.string.action_answer), replyIntent)
                                .addRemoteInput(remoteInput)
                                .setAllowGeneratedReplies(true)
                                .build()

                        addAction(actionReplyByRemoteInput)
                    }
                }
                .build()
    }

    enum class NotificationType(val id: Int) {
        NEWS(1357913213),
        CHAT(1353513312)
    }
}
