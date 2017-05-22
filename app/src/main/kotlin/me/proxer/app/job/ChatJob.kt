package me.proxer.app.job

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.*
import me.proxer.app.event.chat.ChatErrorEvent
import me.proxer.app.event.chat.ChatMessageEvent
import me.proxer.app.event.chat.ChatSynchronizationEvent
import me.proxer.app.fragment.chat.ChatFragment
import me.proxer.app.fragment.chat.ConferencesFragment
import me.proxer.app.helper.NotificationHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.library.entitiy.messenger.Conference
import me.proxer.library.entitiy.messenger.Message
import org.greenrobot.eventbus.EventBus
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

        fun scheduleSynchronization() {
            doSchedule(1L, 100L)
        }

        fun scheduleMessageLoad(conferenceId: String) {
            doSchedule(1L, 100L, conferenceId)
        }

        fun cancel() {
            JobManager.instance().cancelAllForTag(TAG)
        }

        fun isRunning() = JobManager.instance().allJobs.find {
            it is ChatJob && !it.isCanceled && !it.isFinished
        } != null

        private fun reschedule(changes: Boolean = false) {
            if (changes || ChatFragment.isActive) {
                StorageHelper.resetChatInterval()

                doSchedule(3_000L, 4_000L)
            } else if (ConferencesFragment.isActive) {
                StorageHelper.resetChatInterval()

                doSchedule(10_000L, 12_000L)
            } else {
                StorageHelper.incrementChatInterval()

                StorageHelper.chatInterval.let {
                    doSchedule(it, it * 2L)
                }
            }
        }

        private fun doSchedule(startTime: Long, endTime: Long, conferenceId: String? = null) {
            JobRequest.Builder(TAG)
                    .apply {
                        if (startTime != 1L) {
                            setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                        }
                    }
                    .setExecutionWindow(startTime, endTime)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .setPersisted(true)
                    .setExtras(PersistableBundleCompat().apply {
                        if (conferenceId != null) {
                            putString(CONFERENCE_ID_EXTRA, conferenceId)
                        }
                    })
                    .build()
                    .schedule()
        }
    }

    private val conferenceId: String?
        get() = params.extras.getString(CONFERENCE_ID_EXTRA, null)

    override fun onRunJob(params: Params): Result {
        if (StorageHelper.user == null) {
            return Result.FAILURE
        }

        val changes = when (conferenceId) {
            null -> try {
                sendMessages()
                markConferencesAsRead()

                val insertedItems = chatDb.insertOrUpdateConferencesWithMessages(fetchConferences().map {
                    fetchNewMessages(it).let { container ->
                        val existingConference = chatDb.findConference(it.id)
                        val isFullyLoaded = container.isFullyLoaded || existingConference?.isFullyLoaded ?: false

                        ConferenceAssociation(it.toMetaConference(isFullyLoaded), container.messages.asReversed())
                    }
                })

                StorageHelper.areConferencesSynchronized = true

                if (insertedItems.isNotEmpty()) {
                    EventBus.getDefault().post(ChatSynchronizationEvent(insertedItems))
                }

                if (!ConferencesFragment.isActive && !ChatFragment.isActive && insertedItems.isNotEmpty()) {
                    showNotification(context, insertedItems.map { it.conference })
                }

                insertedItems.isNotEmpty()
            } catch (error: Throwable) {
                EventBus.getDefault().post(ChatErrorEvent(ChatSynchronizationException(error)))

                false
            }
            else -> {
                try {
                    fetchMoreMessagesAndInsert(conferenceId!!).let {
                        EventBus.getDefault().post(ChatMessageEvent(it))
                    }
                } catch (error: Throwable) {
                    EventBus.getDefault().post(ChatErrorEvent(ChatMessageException(error)))
                }

                false
            }
        }

        reschedule(changes)

        return Result.SUCCESS
    }

    private fun sendMessages() {
        chatDb.getMessagesToSend().forEach {
            val result = api.messenger().sendMessage(it.conferenceId, it.message)
                    .build()
                    .execute()

            chatDb.removeMessageToSend(it.localId)
            chatDb.markAsRead(it.conferenceId)

            // Per documentation: The api may return some String in case something was wrong.
            if (result != null) {
                throw ChatSendMessageException(it.id)
            }
        }
    }

    private fun markConferencesAsRead() {
        chatDb.getConferencesToMarkAsRead().forEach {
            api.messenger().markConferenceAsRead(it.id)
                    .build()
                    .execute()
        }
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
                it != chatDb.findConference(it.id)?.toNonLocalConference()
            }

            if (changedConferences.size / (page + 1) < CONFERENCES_ON_PAGE) {
                break
            } else {
                page++
            }
        }

        return changedConferences
    }

    private fun fetchNewMessages(conference: Conference): MetaMessageContainer {
        val mostRecentMessage = chatDb.getMostRecentMessage(conference.id)?.toNonLocalMessage()
        val newMessages = mutableListOf<Message>()

        var existingUnreadMessageAmount = chatDb.getUnreadMessageAmount(conference.id, conference.lastReadMessageId)
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
                    return MetaMessageContainer(newMessages, true)
                } else {
                    existingUnreadMessageAmount += fetchedMessages.size
                    nextId = fetchedMessages.first().id
                }
            }

            return MetaMessageContainer(newMessages)
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
                    return MetaMessageContainer(newMessages.filter {
                        it.id.toLong() > mostRecentMessageIdBeforeUpdate
                    }, true)
                } else {
                    existingUnreadMessageAmount += fetchedMessages.size
                    currentMessage = fetchedMessages.last()
                    nextId = fetchedMessages.first().id
                }
            }

            return MetaMessageContainer(newMessages.filter {
                it.id.toLong() > mostRecentMessageIdBeforeUpdate
            })
        }
    }

    private fun fetchMoreMessagesAndInsert(conferenceId: String): LocalConferenceAssociation {
        val fetchedMessages = api.messenger().messages()
                .conferenceId(conferenceId)
                .messageId(chatDb.findOldestMessage(conferenceId)?.id ?: "0")
                .markAsRead(false)
                .build()
                .execute()
                .asReversed()

        val insertedMessages = chatDb.insertOrUpdateMessages(fetchedMessages)
        val conference = when {
            fetchedMessages.size < MESSAGES_ON_PAGE -> chatDb.markConferenceAsFullyLoadedAndGet(conferenceId)
            else -> chatDb.getConference(conferenceId)
        }

        return LocalConferenceAssociation(conference, insertedMessages)
    }

    private fun showNotification(context: Context, changedConferences: Collection<LocalConference>) {
        val unreadMap = chatDb.getUnreadConferences()
                .plus(changedConferences)
                .distinct()
                .associate {
                    it to chatDb.getMostRecentMessages(it.id, it.unreadMessageAmount).asReversed()
                }

        NotificationHelper.showOrUpdateChatNotification(context, unreadMap)
    }

    open class ChatException(val innerError: Throwable) : Exception()
    class ChatSynchronizationException(innerError: Throwable) : ChatException(innerError)
    class ChatMessageException(innerError: Throwable) : ChatException(innerError)
    class ChatSendMessageException(val id: String) : Exception()
}