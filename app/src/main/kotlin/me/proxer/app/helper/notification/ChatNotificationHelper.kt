package me.proxer.app.helper.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.RemoteInput
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.activity.ChatActivity
import me.proxer.app.activity.DashboardActivity
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.entity.chat.LocalMessage
import me.proxer.app.helper.MaterialDrawerHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.app.helper.notification.NotificationHelper.CHAT_CHANNEL
import me.proxer.app.receiver.DirectReplyReceiver
import me.proxer.app.util.Utils
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
object ChatNotificationHelper {

    private const val GROUP = "chat"
    private const val ID = 782373275

    fun showOrUpdate(context: Context, conferenceMap: Map<LocalConference, List<LocalMessage>>) {
        listOf(ID to buildChatSummaryNotification(context, conferenceMap))
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

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID)
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
        val style = NotificationCompat.InboxStyle()
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
                                setSpan(StyleSpan(Typeface.BOLD), 0, sender.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            })
                        }
                    }
                }

        return NotificationCompat.Builder(context, CHAT_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(style)
                .setContentIntent(TaskStackBuilder.create(context)
                        .addNextIntent(DashboardActivity.getSectionIntent(context, MaterialDrawerHelper.DrawerItem.CHAT))
                        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))
                .setDefaults(Notification.DEFAULT_ALL)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setNumber(conferenceAmount)
                .setGroup(GROUP)
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

                NotificationCompat.BigTextStyle()
                        .setBigContentTitle(conference.topic)
                        .setSummaryText(content)
                        .bigText(when (conference.isGroup) {
                            true -> SpannableString("$username $message").apply {
                                setSpan(StyleSpan(Typeface.BOLD), 0, username.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            false -> message
                        })
            }

            else -> when (conference.isGroup) {
                true -> NotificationCompat.MessagingStyle(user.name)
                        .setConversationTitle(conference.topic)
                        .apply {
                            messages.forEach {
                                addMessage(it.message, it.date.time, it.username)
                            }
                        }
                false -> NotificationCompat.InboxStyle()
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
                .addNextIntent(DashboardActivity.getSectionIntent(context, MaterialDrawerHelper.DrawerItem.CHAT))
                .addNextIntent(ChatActivity.getIntent(context, conference))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(context, CHAT_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_proxer)
                .setContentTitle(conference.topic)
                .setContentText(content)
                .setLargeIcon(icon)
                .setStyle(style)
                .setContentIntent(intent)
                .setDefaults(Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND or Notification.DEFAULT_LIGHTS)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setGroup(GROUP)
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

                        val actionReplyByRemoteInput = NotificationCompat.Action.Builder(R.mipmap.ic_launcher,
                                context.getString(R.string.action_answer), replyIntent)
                                .addRemoteInput(remoteInput)
                                .setAllowGeneratedReplies(true)
                                .build()

                        addAction(actionReplyByRemoteInput)
                    }
                }
                .build()
    }
}