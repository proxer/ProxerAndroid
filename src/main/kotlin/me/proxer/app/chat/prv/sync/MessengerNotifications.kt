package me.proxer.app.chat.prv.sync

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.text.set
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.toIconicsColor
import com.mikepenz.iconics.utils.toIconicsSizeDp
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.auth.LocalUser
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.prv.PrvMessengerActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.NotificationUtils.CHAT_CHANNEL
import me.proxer.app.util.Utils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.LocalConferenceMap
import me.proxer.app.util.extension.getQuantityString
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.DrawerItem
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * @author Ruben Gees
 */
object MessengerNotifications : KoinComponent {

    private const val GROUP = "chat"
    private const val ID = 782_373_275

    private val storageHelper by inject<StorageHelper>()

    fun showOrUpdate(context: Context, conferenceMap: LocalConferenceMap) {
        val notifications = conferenceMap.entries
            .asSequence()
            .sortedBy { it.key.date }
            .map { (conference, messages) ->
                conference.id.toInt() to when {
                    messages.isEmpty() -> null
                    else -> buildIndividualChatNotification(context, conference, messages)
                }
            }
            .plus(ID to buildChatSummaryNotification(context, conferenceMap))
            .toList()

        notifications.forEach { (id, notification) ->
            when (notification) {
                null -> NotificationManagerCompat.from(context).cancel(id)
                else -> NotificationManagerCompat.from(context).notify(id, notification)
            }
        }
    }

    fun showError(context: Context, error: Throwable) {
        val errorAction = ErrorUtils.handle(error)

        val intent = errorAction.toIntent()?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        NotificationUtils.showErrorNotification(
            context, ID, CHAT_CHANNEL,
            context.getString(R.string.notification_chat_error_title),
            context.getString(errorAction.message),
            intent
        )
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(ID)

    private fun buildChatSummaryNotification(context: Context, conferenceMap: LocalConferenceMap): Notification? {
        val filteredConferenceMap = conferenceMap.filter { (_, messages) -> messages.isNotEmpty() }

        if (filteredConferenceMap.isEmpty()) {
            return null
        }

        val messageAmount = filteredConferenceMap.values.sumBy { it.size }
        val conferenceAmount = filteredConferenceMap.size
        val messageAmountText = context.getQuantityString(R.plurals.notification_chat_message_amount, messageAmount)
        val conferenceAmountText = context.getQuantityString(
            R.plurals.notification_chat_conference_amount, conferenceAmount
        )

        val title = context.getString(R.string.app_name)
        val content = "$messageAmountText $conferenceAmountText"
        val style = buildSummaryStyle(title, content, filteredConferenceMap)

        val shouldAlert = conferenceMap.keys
            .map { it.date }
            .maxBy { it }
            ?.isAfter(storageHelper.lastChatMessageDate)
            ?: true

        return NotificationCompat.Builder(context, CHAT_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_proxer)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(style)
            .setContentIntent(
                TaskStackBuilder.create(context)
                    .addNextIntent(MainActivity.getSectionIntent(context, DrawerItem.MESSENGER))
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            )
            .setDefaults(Notification.DEFAULT_ALL)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(conferenceAmount)
            .setGroup(GROUP)
            .setOnlyAlertOnce(!shouldAlert)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
    }

    private fun buildSummaryStyle(
        title: String,
        content: String,
        filteredConferenceMap: LocalConferenceMap
    ) = NotificationCompat.InboxStyle()
        .setBigContentTitle(title)
        .setSummaryText(content)
        .also {
            filteredConferenceMap.forEach { (conference, messages) ->
                messages.firstOrNull()?.also { message ->
                    val sender = when {
                        conference.isGroup -> "${conference.topic}: ${message.username} "
                        else -> "${conference.topic} "
                    }

                    it.addLine(SpannableString(sender + message.message).apply {
                        this[0..sender.length] = StyleSpan(Typeface.BOLD)
                    })
                }
            }
        }

    private fun buildIndividualChatNotification(
        context: Context,
        conference: LocalConference,
        messages: List<LocalMessage>
    ): Notification? {
        val user = storageHelper.user

        if (messages.isEmpty() || user == null) {
            return null
        }

        val content = when (messages.size) {
            1 -> messages.first().message
            else -> context.getQuantityString(R.plurals.notification_chat_message_amount, messages.size)
        }

        val icon = buildIndividualIcon(context, conference)
        val style = buildIndividualStyle(messages, conference, user, icon)
        val intent = TaskStackBuilder.create(context)
            .addNextIntent(MainActivity.getSectionIntent(context, DrawerItem.MESSENGER))
            .addNextIntent(PrvMessengerActivity.getIntent(context, conference))
            .getPendingIntent(conference.id.toInt(), PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(context, CHAT_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_proxer)
            .setContentTitle(if (conference.isGroup) conference.topic else "")
            .setContentText(content)
            .setLargeIcon(icon)
            .setStyle(style)
            .setContentIntent(intent)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setAutoCancel(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val remoteInput = RemoteInput.Builder(DirectReplyReceiver.REMOTE_REPLY_EXTRA)
                        .setLabel(context.getString(R.string.action_answer))
                        .build()

                    val replyIntent = DirectReplyReceiver.getPendingIntent(context, conference.id)

                    val actionReplyByRemoteInput = NotificationCompat.Action.Builder(
                        R.mipmap.ic_launcher,
                        context.getString(R.string.action_answer),
                        replyIntent
                    )
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build()

                    addAction(actionReplyByRemoteInput)
                }
            }
            .addAction(
                R.drawable.ic_stat_check,
                context.getString(R.string.notification_chat_read_action),
                MessengerNotificationReadReceiver.getPendingIntent(context, conference.id)
            )
            .build()
    }

    private fun buildIndividualIcon(context: Context, conference: LocalConference) = when {
        conference.image.isNotBlank() -> Utils.getCircleBitmapFromUrl(
            context, ProxerUrls.userImage(conference.image)
        )

        else -> IconicsDrawable(context)
            .icon(
                when (conference.isGroup) {
                    true -> CommunityMaterial.Icon.cmd_account_multiple
                    false -> CommunityMaterial.Icon.cmd_account
                }
            )
            .color(ContextCompat.getColor(context, R.color.primary).toIconicsColor())
            .size(96.toIconicsSizeDp())
            .toBitmap()
    }

    private fun buildIndividualStyle(
        messages: List<LocalMessage>,
        conference: LocalConference,
        user: LocalUser,
        icon: Bitmap?
    ): NotificationCompat.MessagingStyle {
        val person = Person.Builder()
            .apply { if (icon != null) setIcon(IconCompat.createWithBitmap(icon)) }
            .setKey(user.id)
            .setName(user.name)
            .build()

        return NotificationCompat.MessagingStyle(person)
            .setGroupConversation(conference.isGroup)
            .setConversationTitle(if (conference.isGroup) conference.topic else "")
            .also {
                messages.forEach { message ->
                    val messagePerson = Person.Builder()
                        .setName(message.username)
                        .setUri(ProxerUrls.userWeb(message.userId, Device.MOBILE).toString())
                        .setKey(message.userId)
                        .build()

                    it.addMessage(message.message, message.date.toEpochMilli(), messagePerson)
                }
            }
    }
}
