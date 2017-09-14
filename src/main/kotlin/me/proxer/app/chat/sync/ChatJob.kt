package me.proxer.app.chat.sync

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.chatDao
import me.proxer.app.MainApplication.Companion.chatDatabase
import me.proxer.app.chat.ChatFragmentPingEvent
import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.LocalMessage
import me.proxer.app.chat.conference.ConferenceFragmentPingEvent
import me.proxer.app.chat.sync.ChatJob.SynchronizationResult.*
import me.proxer.app.exception.ChatException
import me.proxer.app.exception.ChatMessageException
import me.proxer.app.exception.ChatSendMessageException
import me.proxer.app.exception.ChatSynchronizationException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.toLocalConference
import me.proxer.app.util.extension.toLocalMessage
import me.proxer.library.api.ProxerException
import me.proxer.library.api.ProxerException.ErrorType
import me.proxer.library.api.ProxerException.ServerErrorType
import me.proxer.library.entity.messenger.Conference
import me.proxer.library.entity.messenger.Message
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

        private fun reschedule(context: Context, synchronizationResult: SynchronizationResult) {
            if (canSchedule(context) && synchronizationResult != ERROR) {
                if (synchronizationResult == CHANGES || bus.post(ChatFragmentPingEvent())) {
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
        if (StorageHelper.user == null) return Result.FAILURE

        val synchronizationResult = conferenceId.let { conferenceId ->
            when (conferenceId) {
                0L -> try {
                    val newConferencesAndMessages = synchronize()

                    StorageHelper.areConferencesSynchronized = true

                    if (newConferencesAndMessages.isNotEmpty()) {
                        bus.post(SynchronizationEvent())

                        if (canShowNotification(context)) {
                            showNotification(context)
                        }
                    }

                    newConferencesAndMessages.flatMap { it.value }.maxBy { it.date }?.date?.let { mostRecentDate ->
                        if (mostRecentDate > StorageHelper.lastChatMessageDate) {
                            StorageHelper.lastChatMessageDate = mostRecentDate
                        }
                    }

                    if (newConferencesAndMessages.isNotEmpty()) CHANGES else NO_CHANGES
                } catch (error: Throwable) {
                    when (error) {
                        is ChatException -> bus.post(ChatErrorEvent(error))
                        else -> bus.post(ChatErrorEvent(ChatSynchronizationException(error)))
                    }

                    if (ErrorUtils.isIpBlockedError(error)) {
                        ChatNotifications.showError(context, error)

                        ERROR
                    } else {
                        NO_CHANGES
                    }
                }
                else -> try {
                    val fetchedMessages = loadMoreMessages(conferenceId)

                    fetchedMessages.maxBy { it.date }?.date?.let { mostRecentDate ->
                        if (mostRecentDate > StorageHelper.lastChatMessageDate) {
                            StorageHelper.lastChatMessageDate = mostRecentDate
                        }
                    }

                    CHANGES
                } catch (error: Throwable) {
                    when (error) {
                        is ChatException -> bus.post(ChatErrorEvent(error))
                        else -> bus.post(ChatErrorEvent(ChatMessageException(error)))
                    }

                    NO_CHANGES
                }
            }
        }

        reschedule(context, synchronizationResult)

        return Result.SUCCESS
    }

    private fun synchronize(): Map<LocalConference, List<LocalMessage>> {
        val deletedMessages = sendMessages()

        markConferencesAsRead(chatDao.getConferencesToMarkAsRead()
                .plus(deletedMessages.map { chatDao.findConference(it.conferenceId) })
                .distinct()
                .filterNotNull())

        val newConferencesAndMessages = fetchConferences().associate { conference ->
            fetchNewMessages(conference).let { (messages, isFullyLoaded) ->
                val isLocallyFullyLoaded = chatDao.findConference(conference.id.toLong())
                        ?.isFullyLoaded == true

                conference.toLocalConference(isLocallyFullyLoaded || isFullyLoaded) to
                        messages.map { it.toLocalMessage() }.asReversed()
            }
        }

        chatDatabase.runInTransaction {
            newConferencesAndMessages.let {
                chatDao.insertConferences(it.keys.toList())
                chatDao.insertMessages(it.values.flatten())
            }

            deletedMessages.forEach {
                chatDao.deleteMessageToSend(it.id)
            }
        }

        return newConferencesAndMessages
    }

    private fun loadMoreMessages(conferenceId: Long): List<Message> {
        val fetchedMessages = fetchMoreMessages(conferenceId)

        chatDatabase.runInTransaction {
            chatDao.insertMessages(fetchedMessages.map { it.toLocalMessage() })

            if (fetchedMessages.size < MESSAGES_ON_PAGE) {
                chatDao.markConferenceAsFullyLoaded(conferenceId)
            }
        }

        return fetchedMessages
    }

    private fun sendMessages() = chatDao.getMessagesToSend().apply {
        forEachIndexed { index, (messageId, conferenceId, _, _, message) ->
            val result = api.messenger().sendMessage(conferenceId.toString(), message)
                    .build()
                    .execute()

            // Per documentation: The api may return some String in case something went wrong.
            if (result != null) {

                // Delete all messages we have correctly sent already.
                for (i in 0..index) {
                    chatDao.deleteMessageToSend(get(i).id)
                }

                throw ChatSendMessageException(ProxerException(ErrorType.SERVER,
                        ServerErrorType.MESSAGES_INVALID_MESSAGE, result), messageId)
            }
        }
    }

    private fun markConferencesAsRead(conferenceToMarkAsRead: List<LocalConference>) = conferenceToMarkAsRead.forEach {
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
                    .safeExecute()

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

        return when (mostRecentMessage) {
            null -> fetchNewMessagesForEmptyConference(conference)
            else -> fetchNewMessagesForExistingConference(conference, mostRecentMessage)
        }
    }

    private fun fetchNewMessagesForEmptyConference(conference: Conference): Pair<List<Message>, Boolean> {
        val newMessages = mutableListOf<Message>()

        var unreadAmount = 0
        var nextId = "0"

        while (unreadAmount < conference.unreadMessageAmount) {
            val fetchedMessages = api.messenger().messages()
                    .conferenceId(conference.id)
                    .messageId(nextId)
                    .markAsRead(false)
                    .build()
                    .safeExecute()

            newMessages += fetchedMessages

            if (fetchedMessages.size < MESSAGES_ON_PAGE) {
                return newMessages to true
            } else {
                unreadAmount += fetchedMessages.size
                nextId = fetchedMessages.first().id
            }
        }

        return newMessages to false
    }

    private fun fetchNewMessagesForExistingConference(conference: Conference, mostRecentMessage: Message):
            Pair<List<Message>, Boolean> {
        val mostRecentMessageIdBeforeUpdate = mostRecentMessage.id.toLong()
        val newMessages = mutableListOf<Message>()

        var existingUnreadMessageAmount = chatDao.getUnreadMessageAmountForConference(conference.id.toLong(),
                conference.lastReadMessageId.toLong())
        var currentMessage: Message = mostRecentMessage
        var nextId = "0"

        while (currentMessage.date < conference.date || existingUnreadMessageAmount < conference.unreadMessageAmount) {
            val fetchedMessages = api.messenger().messages()
                    .conferenceId(conference.id)
                    .messageId(nextId)
                    .markAsRead(false)
                    .build()
                    .safeExecute()

            newMessages += fetchedMessages

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

    private fun fetchMoreMessages(conferenceId: Long) = api.messenger().messages()
            .conferenceId(conferenceId.toString())
            .messageId(chatDao.findOldestMessageForConference(conferenceId)?.id?.toString() ?: "0")
            .markAsRead(false)
            .build()
            .safeExecute()
            .asReversed()

    private fun showNotification(context: Context) {
        val unreadMap = chatDao.getUnreadConferences().associate {
            it to chatDao.getMostRecentMessagesForConference(it.id, it.unreadMessageAmount).asReversed()
        }

        ChatNotifications.showOrUpdate(context, unreadMap)
    }

    class SynchronizationEvent

    private enum class SynchronizationResult {
        CHANGES, NO_CHANGES, ERROR
    }
}
