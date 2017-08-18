package me.proxer.app.chat.sync

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.chatDao
import me.proxer.app.chat.ChatFragmentPingEvent
import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.LocalMessage
import me.proxer.app.chat.conference.ConferenceFragmentPingEvent
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.toLocalConference
import me.proxer.app.util.extension.toLocalMessage
import me.proxer.library.api.ProxerException
import me.proxer.library.api.ProxerException.ErrorType
import me.proxer.library.api.ProxerException.ServerErrorType
import me.proxer.library.entitiy.messenger.Conference
import me.proxer.library.entitiy.messenger.Message
import java.util.*

/**
 * @author Ruben Gees
 */
class ChatJob : Job() {

    companion object {
        const val TAG = "chat_job"

        const val CONFERENCES_ON_PAGE = 48
        const val MESSAGES_ON_PAGE = 30

        private const val CONFERENCE_ID_EXTRA = "conference_id"

        fun scheduleSynchronizationIfPossible(context: Context) = if (canSchedule(context)) {
            scheduleSynchronization()
        } else {
            cancel()
        }

        fun scheduleSynchronization() = doSchedule(1L, 100L)

        fun scheduleMessageLoad(conferenceId: Long) = doSchedule(1L, 100L, conferenceId)

        fun cancel() {
            JobManager.instance().cancelAllForTag(TAG)
        }

        fun isRunning() = JobManager.instance().allJobs.find {
            it is ChatJob && !it.isCanceled && !it.isFinished
        } != null

        private fun reschedule(context: Context, changes: Boolean = false) {
            if (canSchedule(context)) {
                if (changes || bus.post(ChatFragmentPingEvent())) {
                    StorageHelper.resetChatInterval()

                    doSchedule(3_000L, 4_000L)
                } else if (bus.post(ConferenceFragmentPingEvent())) {
                    StorageHelper.resetChatInterval()

                    doSchedule(10_000L, 12_000L)
                } else {
                    StorageHelper.incrementChatInterval()

                    StorageHelper.chatInterval.let {
                        doSchedule(it, it * 2L)
                    }
                }
            }
        }

        private fun doSchedule(startTime: Long, endTime: Long, conferenceId: Long? = null) {
            JobRequest.Builder(TAG)
                    .apply {
                        if (startTime != 1L) {
                            setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                        }
                    }
                    .setExecutionWindow(startTime, endTime)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .setExtras(PersistableBundleCompat().apply {
                        if (conferenceId != null) {
                            putLong(CONFERENCE_ID_EXTRA, conferenceId)
                        }
                    })
                    .build()
                    .schedule()
        }

        private fun canSchedule(context: Context) = PreferenceHelper.areChatNotificationsEnabled(context)
                || bus.post(ConferenceFragmentPingEvent()) || bus.post(ChatFragmentPingEvent())

        private fun canShowNotification(context: Context) = PreferenceHelper.areChatNotificationsEnabled(context)
                && !bus.post(ConferenceFragmentPingEvent()) && !bus.post(ChatFragmentPingEvent())
    }

    private val conferenceId: Long
        get() = params.extras.getLong(CONFERENCE_ID_EXTRA, 0)

    override fun onRunJob(params: Params): Result {
        if (StorageHelper.user == null) {
            return Result.FAILURE
        }

        val changes = conferenceId.let { conferenceId ->
            when (conferenceId) {
                0L -> try {
                    sendMessages()
                    markConferencesAsRead()

                    val fetchedConferences = fetchConferences()
                    val fetchedMessages = fetchedConferences.map { fetchNewMessages(it) }

                    val conferences = fetchedConferences.mapIndexed { index, it ->
                        it.toLocalConference(fetchedMessages[index].second
                                || chatDao.findConference(it.id.toLong())?.isFullyLoaded == true)
                    }

                    val messages = fetchedMessages.flatMap { it.first }
                            .map { it.toLocalMessage() }
                            .asReversed()

                    chatDao.insertConferences(conferences)
                    chatDao.insertMessages(messages)

                    val dataMap = conferences.associate { conference ->
                        conference to messages.filter { it.conferenceId == conference.id }
                    }

                    StorageHelper.areConferencesSynchronized = true

                    if (dataMap.isNotEmpty()) {
                        bus.post(ChatSynchronizationEvent(dataMap))

                        if (canShowNotification(context)) {
                            showNotification(context, conferences)
                        }
                    }

                    dataMap.isNotEmpty()
                } catch (error: Throwable) {
                    when (error) {
                        is ChatException -> bus.post(ChatErrorEvent(error))
                        else -> bus.post(ChatErrorEvent(ChatSynchronizationException(error)))
                    }

                    false
                }
                else -> {
                    try {
                        fetchMoreMessagesAndInsert(conferenceId).let { bus.post(ChatMessageEvent(it)) }
                    } catch (error: Throwable) {
                        when (error) {
                            is ChatException -> bus.post(ChatErrorEvent(error))
                            else -> bus.post(ChatErrorEvent(ChatMessageException(error)))
                        }
                    }

                    false
                }
            }
        }

        reschedule(context, changes)

        return Result.SUCCESS
    }

    //
    private fun sendMessages() = chatDao.getMessagesToSend().forEach {
        val result = api.messenger().sendMessage(it.conferenceId.toString(), it.message)
                .build()
                .execute()

        chatDao.deleteMessageToSend(it.id)
        chatDao.markConferenceAsRead(it.conferenceId)

        // Per documentation: The api may return some String in case something was wrong.
        if (result != null) {
            throw ChatSendMessageException(ProxerException(ErrorType.SERVER,
                    ServerErrorType.MESSAGES_INVALID_MESSAGE, result), it.id)
        }
    }

    private fun markConferencesAsRead() = chatDao.getConferencesToMarkAsRead().forEach {
        api.messenger().markConferenceAsRead(it.id.toString())
                .build()
                .execute()
    }

    private fun fetchConferences(): Collection<Conference> {
        val changedConferences = LinkedHashSet<Conference>()
        var page = 0

        while (true) {
            val fetchedConferences = api.messenger().conferences()
                    .page(page)
                    .build()
                    .execute()

            changedConferences += fetchedConferences.filter {
                it != chatDao.findConference(it.id.toLong())?.toNonLocalConference()
            }

            if (changedConferences.size / (page + 1) < CONFERENCES_ON_PAGE) {
                break
            } else {
                page++
            }
        }

        return changedConferences
    }

    private fun fetchNewMessages(conference: Conference): Pair<List<Message>, Boolean> {
        val mostRecentMessage = chatDao.findMostRecentMessageForConference(conference.id.toLong())?.toNonLocalMessage()
        val newMessages = mutableListOf<Message>()

        var existingUnreadMessageAmount = chatDao.getUnreadMessageAmountForConference(conference.id.toLong(),
                conference.lastReadMessageId.toLong())
        var nextId = "0"

        if (mostRecentMessage == null) {
            while (existingUnreadMessageAmount < conference.unreadMessageAmount) {
                val fetchedMessages = api.messenger().messages()
                        .conferenceId(conference.id)
                        .messageId(nextId)
                        .markAsRead(false)
                        .build()
                        .execute()

                newMessages += fetchedMessages

                if (fetchedMessages.size < MESSAGES_ON_PAGE) {
                    return newMessages to true
                } else {
                    existingUnreadMessageAmount += fetchedMessages.size
                    nextId = fetchedMessages.first().id
                }
            }

            return newMessages to false
        } else {
            val mostRecentMessageIdBeforeUpdate = mostRecentMessage.id.toLong()
            var currentMessage: Message = mostRecentMessage

            while (currentMessage.date < conference.date ||
                    existingUnreadMessageAmount < conference.unreadMessageAmount) {

                val fetchedMessages = api.messenger().messages()
                        .conferenceId(conference.id)
                        .messageId(nextId)
                        .markAsRead(false)
                        .build()
                        .execute()

                newMessages.addAll(fetchedMessages)

                if (fetchedMessages.size < MESSAGES_ON_PAGE) {
                    return newMessages.filter { it.id.toLong() > mostRecentMessageIdBeforeUpdate } to true
                } else {
                    existingUnreadMessageAmount += fetchedMessages.size
                    currentMessage = fetchedMessages.last()
                    nextId = fetchedMessages.first().id
                }
            }

            return newMessages.filter { it.id.toLong() > mostRecentMessageIdBeforeUpdate } to false
        }
    }

    private fun fetchMoreMessagesAndInsert(conferenceId: Long): Pair<LocalConference, List<LocalMessage>> {
        val fetchedMessages = api.messenger().messages()
                .conferenceId(conferenceId.toString())
                .messageId(chatDao.findOldestMessageForConference(conferenceId)?.id?.toString() ?: "0")
                .markAsRead(false)
                .build()
                .execute()
                .asReversed()

        val insertedMessages = chatDao.insertMessages(fetchedMessages.map { it.toLocalMessage() })
                .mapIndexed { index, _ -> fetchedMessages[index].toLocalMessage() }

        if (fetchedMessages.size < MESSAGES_ON_PAGE) {
            chatDao.markConferenceAsFullyLoaded(conferenceId)
        }

        val conference = chatDao.findConference(conferenceId)
                ?: throw IllegalStateException("Conference (id: $conferenceId) cannot be null.")

        return conference to insertedMessages
    }

    private fun showNotification(context: Context, changedConferences: Collection<LocalConference>) {
        val unreadMap = chatDao.getUnreadConferences()
                .plus(changedConferences)
                .distinct()
                .associate {
                    it to chatDao.getMostRecentMessagesForConference(it.id, it.unreadMessageAmount).asReversed()
                }

        ChatNotifications.showOrUpdate(context, unreadMap)
    }

    open class ChatException(val innerError: Throwable) : Exception()
    class ChatSynchronizationException(innerError: Throwable) : ChatException(innerError)
    class ChatMessageException(innerError: Throwable) : ChatException(innerError)
    class ChatSendMessageException(innerError: Throwable, val id: Long) : ChatException(innerError)
}
