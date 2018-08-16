package me.proxer.app.chat.prv.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.State
import androidx.work.WorkManager
import androidx.work.Worker
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.messengerDao
import me.proxer.app.MainApplication.Companion.messengerDatabase
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.prv.conference.ConferenceFragmentPingEvent
import me.proxer.app.chat.prv.message.MessengerFragmentPingEvent
import me.proxer.app.exception.ChatException
import me.proxer.app.exception.ChatMessageException
import me.proxer.app.exception.ChatSendMessageException
import me.proxer.app.exception.ChatSynchronizationException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.WorkerUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.toLocalConference
import me.proxer.app.util.extension.toLocalMessage
import me.proxer.library.api.ProxerCall
import me.proxer.library.api.ProxerException
import me.proxer.library.entity.messenger.Conference
import me.proxer.library.entity.messenger.Message
import java.util.LinkedHashSet
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class MessengerWorker : Worker() {

    companion object {
        private const val NAME = "MessengerWorker"
        private const val CONFERENCE_ID_ARGUMENT = "conference_id"

        const val CONFERENCES_ON_PAGE = 48
        const val MESSAGES_ON_PAGE = 30

        fun enqueueSynchronizationIfPossible(context: Context) = if (canSchedule(context)) {
            enqueueSynchronization()
        } else {
            cancel()
        }

        fun enqueueSynchronization() = doEnqueue()

        fun enqueueMessageLoad(conferenceId: Long) = doEnqueue(conferenceId = conferenceId)

        fun cancel() {
            WorkManager.getInstance().cancelUniqueWork(NAME)
        }

        fun isRunning() = WorkManager.getInstance().getStatusesForUniqueWork(NAME)
            .value?.firstOrNull()?.state == State.RUNNING

        private fun reschedule(context: Context, synchronizationResult: SynchronizationResult) {
            if (canSchedule(context) && synchronizationResult != SynchronizationResult.ERROR) {
                if (synchronizationResult == SynchronizationResult.CHANGES || bus.post(MessengerFragmentPingEvent())) {
                    StorageHelper.resetChatInterval()

                    doEnqueue(3_000L)
                } else if (bus.post(ConferenceFragmentPingEvent())) {
                    StorageHelper.resetChatInterval()

                    doEnqueue(10_000L)
                } else {
                    StorageHelper.incrementChatInterval()

                    doEnqueue(StorageHelper.chatInterval)
                }
            }
        }

        private fun doEnqueue(startTime: Long? = null, conferenceId: Long? = null) {
            WorkManager.getInstance().beginUniqueWork(
                NAME, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<MessengerWorker>()
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                    .apply { if (startTime != null) setInitialDelay(startTime, TimeUnit.MILLISECONDS) }
                    .setInputData(Data.Builder()
                        .apply { if (conferenceId != null) putLong(CONFERENCE_ID_ARGUMENT, conferenceId) }
                        .build())
                    .build())
                .enqueue()
        }

        private fun canSchedule(context: Context) = PreferenceHelper.areChatNotificationsEnabled(context) ||
            bus.post(ConferenceFragmentPingEvent()) || bus.post(MessengerFragmentPingEvent())

        private fun canShowNotification(context: Context) = PreferenceHelper.areChatNotificationsEnabled(context) &&
            !bus.post(ConferenceFragmentPingEvent()) && !bus.post(MessengerFragmentPingEvent())
    }

    private val conferenceId: Long
        get() = inputData.getLong(CONFERENCE_ID_ARGUMENT, 0L)

    private var currentCall: ProxerCall<*>? = null

    override fun onStopped(cancelled: Boolean) {
        currentCall?.cancel()
    }

    override fun doWork(): Result {
        if (!StorageHelper.isLoggedIn) return Result.FAILURE

        val synchronizationResult = when (conferenceId) {
            0L -> try {
                handleSynchronization()
            } catch (error: Throwable) {
                handleSynchronizationError(error)
            }
            else -> try {
                handleLoadMoreMessages(conferenceId)
            } catch (error: Throwable) {
                handleLoadMoreMessagesError(error)
            }
        }

        reschedule(applicationContext, synchronizationResult)

        return if (synchronizationResult != SynchronizationResult.ERROR) Result.SUCCESS else Result.FAILURE
    }

    private fun handleSynchronization(): SynchronizationResult {
        val newConferencesAndMessages = synchronize()

        StorageHelper.areConferencesSynchronized = true

        if (newConferencesAndMessages.isNotEmpty() && !isStopped) {
            bus.post(SynchronizationEvent())

            if (canShowNotification(applicationContext)) {
                showNotification(applicationContext)
            }
        }

        newConferencesAndMessages.flatMap { it.value }.maxBy { it.date }?.date?.let { mostRecentDate ->
            if (mostRecentDate > StorageHelper.lastChatMessageDate) {
                StorageHelper.lastChatMessageDate = mostRecentDate
            }
        }

        return when (newConferencesAndMessages.isNotEmpty()) {
            true -> SynchronizationResult.CHANGES
            false -> SynchronizationResult.NO_CHANGES
        }
    }

    private fun handleSynchronizationError(error: Throwable): SynchronizationResult {
        if (!isStopped) {
            when (error) {
                is ChatException -> bus.post(MessengerErrorEvent(error))
                else -> bus.post(MessengerErrorEvent(ChatSynchronizationException(error)))
            }
        }

        return if (!isStopped && WorkerUtils.shouldShowError(runAttemptCount, error)) {
            if (canShowNotification(applicationContext)) {
                MessengerNotifications.showError(applicationContext, error)
            }

            SynchronizationResult.ERROR
        } else {
            SynchronizationResult.NO_CHANGES
        }
    }

    private fun handleLoadMoreMessagesError(error: Throwable): SynchronizationResult {
        if (!isStopped) {
            when (error) {
                is ChatException -> bus.post(MessengerErrorEvent(error))
                else -> bus.post(MessengerErrorEvent(ChatMessageException(error)))
            }
        }

        return when (!isStopped && ErrorUtils.isIpBlockedError(error)) {
            true -> SynchronizationResult.ERROR
            false -> SynchronizationResult.NO_CHANGES
        }
    }

    private fun handleLoadMoreMessages(conferenceId: Long): SynchronizationResult {
        val fetchedMessages = loadMoreMessages(conferenceId)

        fetchedMessages.maxBy { it.date }?.date?.let { mostRecentDate ->
            if (mostRecentDate > StorageHelper.lastChatMessageDate) {
                StorageHelper.lastChatMessageDate = mostRecentDate
            }
        }

        return SynchronizationResult.CHANGES
    }

    @Suppress("RethrowCaughtException")
    private fun synchronize(): Map<LocalConference, List<LocalMessage>> {
        val sentMessages = sendMessages()

        val newConferencesAndMessages = try {
            markConferencesAsRead(messengerDao.getConferencesToMarkAsRead()
                .plus(sentMessages.map { messengerDao.findConference(it.conferenceId) })
                .distinct()
                .filterNotNull())

            fetchConferences().associate { conference ->
                fetchNewMessages(conference).let { (messages, isFullyLoaded) ->
                    val isLocallyFullyLoaded = messengerDao.findConference(conference.id.toLong())
                        ?.isFullyLoaded == true

                    conference.toLocalConference(isLocallyFullyLoaded || isFullyLoaded) to
                        messages.map { it.toLocalMessage() }.asReversed()
                }
            }
        } catch (error: Throwable) {
            messengerDatabase.runInTransaction {
                sentMessages.forEach {
                    messengerDao.deleteMessageToSend(it.id)
                }
            }

            throw error
        }

        messengerDatabase.runInTransaction {
            newConferencesAndMessages.let {
                messengerDao.insertConferences(it.keys.toList())
                messengerDao.insertMessages(it.values.flatten())
            }

            sentMessages.forEach {
                messengerDao.deleteMessageToSend(it.id)
            }
        }

        return newConferencesAndMessages
    }

    private fun loadMoreMessages(conferenceId: Long): List<Message> {
        val fetchedMessages = fetchMoreMessages(conferenceId)

        messengerDatabase.runInTransaction {
            messengerDao.insertMessages(fetchedMessages.map { it.toLocalMessage() })

            if (fetchedMessages.size < MESSAGES_ON_PAGE) {
                messengerDao.markConferenceAsFullyLoaded(conferenceId)
            }
        }

        return fetchedMessages
    }

    @Suppress("RethrowCaughtException")
    private fun sendMessages() = messengerDao.getMessagesToSend().apply {
        forEachIndexed { index, (messageId, conferenceId, _, _, message) ->
            val result = try {
                api.messenger().sendMessage(conferenceId.toString(), message)
                    .build()
                    .also { currentCall = it }
                    .execute()
            } catch (error: ProxerException) {
                if (error.cause?.stackTrace?.find { it.methodName.contains("read") } != null) {
                    // The message was sent, but we did not receive a proper api answer due to slow network, return
                    // non-null to handle it like the non-empty result case.
                    "error"
                } else {
                    // The message was most likely not sent, but the previous ones are. Delete them to avoid resending.
                    messengerDatabase.runInTransaction {
                        for (i in 0 until index) {
                            messengerDao.deleteMessageToSend(get(i).id)
                        }
                    }

                    throw error
                }
            }

            // Per documentation: The api may return some String in case something went wrong.
            if (result != null) {
                // Delete all messages we have correctly sent already.
                messengerDatabase.runInTransaction {
                    for (i in 0..index) {
                        messengerDao.deleteMessageToSend(get(i).id)
                    }
                }

                throw ChatSendMessageException(ProxerException(ProxerException.ErrorType.SERVER,
                    ProxerException.ServerErrorType.MESSAGES_INVALID_MESSAGE, result), messageId)
            }
        }
    }

    private fun markConferencesAsRead(conferenceToMarkAsRead: List<LocalConference>) = conferenceToMarkAsRead.forEach {
        api.messenger().markConferenceAsRead(it.id.toString())
            .build()
            .also { call -> currentCall = call }
            .execute()
    }

    private fun fetchConferences(): Collection<Conference> {
        val changedConferences = LinkedHashSet<Conference>()
        var page = 0

        while (true) {
            val fetchedConferences = api.messenger().conferences()
                .page(page)
                .build()
                .also { currentCall = it }
                .safeExecute()

            changedConferences += fetchedConferences.filter {
                it != messengerDao.findConference(it.id.toLong())?.toNonLocalConference()
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
        val mostRecentMessage = messengerDao.findMostRecentMessageForConference(conference.id.toLong())
            ?.toNonLocalMessage()

        return when (mostRecentMessage) {
            null -> fetchForEmptyConference(conference)
            else -> fetchForExistingConference(conference, mostRecentMessage)
        }
    }

    private fun fetchForEmptyConference(conference: Conference): Pair<List<Message>, Boolean> {
        val newMessages = mutableListOf<Message>()

        var unreadAmount = 0
        var nextId = "0"

        while (unreadAmount < conference.unreadMessageAmount) {
            val fetchedMessages = api.messenger().messages()
                .conferenceId(conference.id)
                .messageId(nextId)
                .markAsRead(false)
                .build()
                .also { currentCall = it }
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

    private fun fetchForExistingConference(
        conference: Conference,
        mostRecentMessage: Message
    ): Pair<List<Message>, Boolean> {
        val mostRecentMessageIdBeforeUpdate = mostRecentMessage.id.toLong()
        val newMessages = mutableListOf<Message>()

        var existingUnreadMessageAmount = messengerDao.getUnreadMessageAmountForConference(conference.id.toLong(),
            conference.lastReadMessageId.toLong())
        var currentMessage: Message = mostRecentMessage
        var nextId = "0"

        while (currentMessage.date < conference.date || existingUnreadMessageAmount < conference.unreadMessageAmount) {
            val fetchedMessages = api.messenger().messages()
                .conferenceId(conference.id)
                .messageId(nextId)
                .markAsRead(false)
                .build()
                .also { currentCall = it }
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
        .messageId(messengerDao.findOldestMessageForConference(conferenceId)?.id?.toString() ?: "0")
        .markAsRead(false)
        .build()
        .also { currentCall = it }
        .safeExecute()
        .asReversed()

    private fun showNotification(context: Context) {
        val unreadMap = messengerDao.getUnreadConferences().associate {
            it to messengerDao.getMostRecentMessagesForConference(it.id, it.unreadMessageAmount).asReversed()
        }

        MessengerNotifications.showOrUpdate(context, unreadMap)
    }

    class SynchronizationEvent

    private enum class SynchronizationResult {
        CHANGES, NO_CHANGES, ERROR
    }
}
