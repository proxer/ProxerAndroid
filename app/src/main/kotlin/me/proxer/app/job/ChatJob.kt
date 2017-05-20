package me.proxer.app.job

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.event.ChatErrorEvent
import me.proxer.app.event.ChatMessageEvent
import me.proxer.app.event.ChatSynchronizationEvent
import me.proxer.app.fragment.chat.ChatFragment
import me.proxer.app.fragment.chat.ConferencesFragment
import me.proxer.app.helper.NotificationHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.library.api.ProxerException
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

        private fun reschedule() {
            if (ChatFragment.isActive) {
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
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
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

        try {
            if (conferenceId == null) {
                sendMessages()
                markConferencesAsRead()

                val insertedMap = chatDb.insertOrUpdate(fetchConferences().associate { conference ->
                    fetchNewMessages(conference).let { conference to it.second to it.first }
                })

                EventBus.getDefault().post(ChatSynchronizationEvent(insertedMap))

                if (!ConferencesFragment.isActive && !ChatFragment.isActive && insertedMap.isNotEmpty()) {
                    showNotification(context, insertedMap.keys)
                }
            } else {
                conferenceId?.let {
                    val fetchedMessages = api.messenger().messages()
                            .conferenceId(it)
                            .messageId(chatDb.getOldestMessage(it)?.id ?: "0")
                            .markAsRead(false)
                            .build()
                            .execute()
                            .asReversed()

                    val insertedMessages = chatDb.insertOrUpdateMessages(fetchedMessages)
                    val conference = when {
                        fetchedMessages.size < MESSAGES_ON_PAGE -> chatDb.markConferenceAsFullyLoaded(it)
                        else -> chatDb.getConference(it)
                    }

                    EventBus.getDefault().post(ChatMessageEvent(conference, insertedMessages))
                }
            }
        } catch(error: ChatException) {
            EventBus.getDefault().post(ChatErrorEvent(error))
        } catch (ignored: Throwable) {
        }

        reschedule()

        return Result.SUCCESS
    }

    private fun sendMessages() {
        chatDb.getMessagesToSend().forEach {
            try {
                val result = api.messenger().sendMessage(it.conferenceId, it.message)
                        .build()
                        .execute()

                if (result != null) {
                    throw ChatException(ProxerException(ProxerException.ErrorType.UNKNOWN))
                }

                chatDb.removeMessageToSend(it.localId)
                chatDb.markAsRead(it.conferenceId)
            } catch(exception: Exception) {
                throw ChatException(exception)
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
        try {
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

            StorageHelper.hasConferenceListReachedEnd = true

            return changedConferences
        } catch (exception: Exception) {
            throw ChatException(exception)
        }
    }

    private fun fetchNewMessages(conference: Conference): Pair<List<Message>, Boolean> {
        val newMessages = ArrayList<Message>()

        try {
            var existingUnreadMessageAmount = chatDb.getUnreadMessageAmount(conference.id, conference.lastReadMessageId)
            var mostRecentMessage = chatDb.getMostRecentMessage(conference.id)?.toNonLocalMessage()
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
            } else {
                while (mostRecentMessage!!.date < conference.date ||
                        existingUnreadMessageAmount < conference.unreadMessageAmount) {

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
                        mostRecentMessage = fetchedMessages.last()
                        nextId = fetchedMessages.first().id
                    }
                }
            }
        } catch (exception: Exception) {
            throw ChatException(exception)
        }

        return newMessages to false
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

    class ChatException(val innerError: Exception) : Exception()
}