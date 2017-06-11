package me.proxer.app.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.support.v4.app.NotificationCompat.*
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.RemoteInput
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import android.support.v7.app.NotificationCompat
import android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableString
import android.text.style.StyleSpan
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.activity.ChatActivity
import me.proxer.app.activity.DashboardActivity
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.entity.chat.LocalMessage
import me.proxer.app.fragment.news.NewsArticleFragment
import me.proxer.app.helper.MaterialDrawerHelper.DrawerItem
import me.proxer.app.receiver.DirectReplyReceiver
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Utils
import me.proxer.library.entitiy.notifications.NewsArticle
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
object NotificationHelper {

    private const val GROUP_CHAT = "chat"

    private const val CHANNEL_ACCOUNT = "proxer_account"
    private const val CHANNEL_CHAT = "proxer_chat"
    private const val CHANNEL_ERRORS = "proxer_errors"

    private const val TYPE_NEWS = 1357913213
    private const val TYPE_NEWS_ERROR = 472347289
    private const val TYPE_CHAT = 782373275
    private const val TYPE_MANGA_DOWNLOAD_ERROR = 479239223

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val accountTitle = context.getString(R.string.notification_channel_account)
            val chatTitle = context.getString(R.string.notification_channel_chat)
            val errorsTitle = context.getString(R.string.notification_channel_errors)

            notificationManager.createNotificationChannels(listOf(
                    NotificationChannel(CHANNEL_ACCOUNT, accountTitle, NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = context.getString(R.string.notification_channel_account_description)
                    },
                    NotificationChannel(CHANNEL_CHAT, chatTitle, NotificationManager.IMPORTANCE_HIGH).apply {
                        description = context.getString(R.string.notification_channel_chat_description)
                    },
                    NotificationChannel(CHANNEL_ERRORS, errorsTitle, NotificationManager.IMPORTANCE_LOW).apply {
                        description = context.getString(R.string.notification_channel_errors_description)
                    }
            ))
        }
    }

    fun showOrUpdateNewsNotification(context: Context, news: Collection<NewsArticle>) {
        if (!NewsArticleFragment.isActive) {
            val notification = buildNewsNotification(context, news)

            when (notification) {
                null -> NotificationManagerCompat.from(context).cancel(TYPE_NEWS)
                else -> NotificationManagerCompat.from(context).notify(TYPE_NEWS, notification)
            }
        }
    }

    fun showNewsErrorNotification(context: Context, error: Throwable) {
        showErrorNotification(context, TYPE_NEWS,
                context.getString(R.string.notification_news_error_title),
                context.getString(ErrorUtils.getMessage(error)))
    }

    fun showOrUpdateChatNotification(context: Context, conferenceMap: Map<LocalConference, List<LocalMessage>>) {
        listOf(TYPE_CHAT to buildChatSummaryNotification(context, conferenceMap))
                .plus(conferenceMap.entries.map { (conference, messages) ->
                    conference.id.toInt() to when {
                        messages.isEmpty() -> null
                        else -> buildIndividualChatNotification(context, conference, messages)
                    }
                }).forEach {
            when (it.second) {
                null -> NotificationManagerCompat.from(context).cancel(it.first)
                else -> NotificationManagerCompat.from(context).notify(it.first, it.second)
            }
        }
    }

    fun showMangaDownloadErrorNotification(context: Context, error: Throwable) {
        showErrorNotification(context, TYPE_MANGA_DOWNLOAD_ERROR,
                context.getString(R.string.notification_manga_download_error_title),
                context.getString(ErrorUtils.getMessage(error)))
    }

    fun cancelNewsNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(TYPE_NEWS)
        NotificationManagerCompat.from(context).cancel(TYPE_NEWS_ERROR)
    }

    fun cancelChatNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(TYPE_CHAT)
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
                .setPriority(PRIORITY_DEFAULT)
                .setChannelId(CHANNEL_ACCOUNT)
                .setNumber(news.size)
                .setStyle(style)
                .build()
    }

    private fun buildChatSummaryNotification(context: Context,
                                             conferenceMap: Map<LocalConference, List<LocalMessage>>): Notification? {

        val filteredConferenceMap = conferenceMap.filter { it.value.isNotEmpty() }.apply {
            if (isEmpty()) {
                return null
            }
        }

        val messageAmount = filteredConferenceMap.values.sumBy { it.size }
        val conferenceAmount = filteredConferenceMap.size
        val messageAmountText = context.resources.getQuantityString(R.plurals.notification_chat_message_amount,
                messageAmount, messageAmount)
        val conferenceAmountText = context.resources.getQuantityString(R.plurals.notification_chat_conference_amount,
                conferenceAmount, conferenceAmount)

        val title = "$messageAmountText $conferenceAmountText"
        val content = SpannableString(filteredConferenceMap.keys.joinToString(", ", transform = { it.topic }))
        val style = InboxStyle()
                .setBigContentTitle(content)
                .setSummaryText(title)
                .apply {
                    filteredConferenceMap.forEach { entry ->
                        entry.value.forEach {
                            val sender = when {
                                entry.key.isGroup -> "${entry.key.topic}: ${it.username} "
                                else -> "${entry.key.topic}: "
                            }

                            addLine(SpannableString(sender + it.message).apply {
                                setSpan(StyleSpan(Typeface.BOLD), 0, sender.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                            })
                        }
                    }
                }

        return NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(style)
                .setContentIntent(TaskStackBuilder.create(context)
                        .addNextIntent(DashboardActivity.getSectionIntent(context, DrawerItem.CHAT))
                        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))
                .setDefaults(Notification.DEFAULT_ALL)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setCategory(CATEGORY_MESSAGE)
                .setPriority(PRIORITY_HIGH)
                .setChannelId(CHANNEL_CHAT)
                .setNumber(conferenceAmount)
                .setGroup(GROUP_CHAT)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()
    }

    private fun buildIndividualChatNotification(context: Context, conference: LocalConference,
                                                messages: List<LocalMessage>): Notification? {
        val user = StorageHelper.user

        if (messages.isEmpty() || user == null) {
            return null
        }

        val amount = messages.size
        val content = context.resources.getQuantityString(R.plurals.notification_chat_message_amount, amount, amount)

        val icon = when {
            conference.image.isNotBlank() -> Utils.getBitmapFromUrl(context,
                    ProxerUrls.userImage(conference.image).toString())

            else -> IconicsDrawable(context, when (conference.isGroup) {
                true -> CommunityMaterial.Icon.cmd_account_multiple
                false -> CommunityMaterial.Icon.cmd_account
            }).sizeDp(96).colorRes(R.color.colorPrimary).toBitmap()
        }

        val style = when (amount) {
            1 -> {
                val username = messages.first().username
                val message = messages.first().message

                BigTextStyle()
                        .setBigContentTitle(conference.topic)
                        .setSummaryText(content)
                        .bigText(when (conference.isGroup) {
                            true -> SpannableString("$username $message").apply {
                                setSpan(StyleSpan(Typeface.BOLD), 0, username.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            false -> message
                        })
            }

            else -> when (conference.isGroup) {
                true -> MessagingStyle(user.name)
                        .setConversationTitle(conference.topic)
                        .apply {
                            messages.forEach {
                                addMessage(it.message, it.date.time, it.username)
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
                .setDefaults(Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND or Notification.DEFAULT_LIGHTS)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(PRIORITY_HIGH)
                .setCategory(CATEGORY_MESSAGE)
                .setChannelId(CHANNEL_CHAT)
                .setGroup(GROUP_CHAT)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val remoteInput = RemoteInput.Builder(DirectReplyReceiver.REMOTE_REPLY_EXTRA)
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

    private fun showErrorNotification(context: Context, id: Int, title: String, content: String) {
        NotificationManagerCompat.from(context).notify(id, NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(content)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setChannelId(CHANNEL_ERRORS)
                .setPriority(PRIORITY_LOW)
                .setAutoCancel(true)
                .build())
    }
}
