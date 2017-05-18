package me.proxer.app.job

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.event.ChatErrorEvent
import me.proxer.app.event.ChatEvent
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

        private const val CONFERENCES_ON_PAGE = 48
        private const val MESSAGES_ON_PAGE = 30

        fun schedule() {
            doSchedule(1L, 100L)
        }

        fun cancel() {
            JobManager.instance().cancelAllForTag(TAG)
        }

        fun isRunning() = JobManager.instance().allJobs.find {
            it is ChatJob && !it.isCanceled && !it.isFinished
        } != null

        private fun reschedule() {
            /* if (ChatFragment.isActive) {
                StorageHelper.resetChatInterval()

                doSchedule(3_000L, 4_000L)
            } else */ if (ConferencesFragment.isActive) {
                StorageHelper.resetChatInterval()

                doSchedule(10_000L, 12_000L)
            } else {
                StorageHelper.incrementChatInterval()

                StorageHelper.chatInterval.let {
                    doSchedule(it, it * 2L)
                }
            }
        }

        private fun doSchedule(startTime: Long, endTime: Long) {
            JobRequest.Builder(TAG)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setExecutionWindow(startTime, endTime)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .setPersisted(true)
                    .build()
                    .schedule()
        }
    }

    override fun onRunJob(params: Params): Result {
        if (StorageHelper.user == null) {
            return Result.FAILURE
        }

        try {
            sendMessages()
            markConferencesAsRead()

            val insertedMap = chatDb.insertOrUpdate(fetchConferences().associate {
                it to fetchMessages(it).asReversed()
            })

            EventBus.getDefault().post(ChatEvent(insertedMap))

            if (!ConferencesFragment.isActive /* && !ChatFragment.isActive */ && insertedMap.isNotEmpty()) {
                showNotification(context, insertedMap.keys)
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

                chatDb.removeMessageToSend(it.localId)
                chatDb.markAsRead(it.conferenceId)

                if (result != null) {
                    throw ChatException(ProxerException(ProxerException.ErrorType.UNKNOWN))
                }
            } catch(exception: Exception) {
                throw ChatException(exception)
            }
        }
    }

    private fun markConferencesAsRead() {
        chatDb.getConferencesToMark().forEach {
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
                    it != chatDb.getConference(it.id)?.toNonLocalConference()
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

    private fun fetchMessages(conference: Conference): List<Message> {
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
                        StorageHelper.setConferenceReachedEnd(conference.id)

                        break
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
                        StorageHelper.setConferenceReachedEnd(conference.id)

                        break
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

        return newMessages
    }

    private fun showNotification(context: Context, changedConferences: Collection<LocalConference>) {
        val unreadMap = chatDb.getUnreadConferences()
                .plus(changedConferences)
                .distinct()
                .associate {
                    it to chatDb.getMostRecentMessages(it.id, it.unreadMessageAmount).reversed()
                }

        NotificationHelper.showOrUpdateChatNotification(context, unreadMap)
    }

    class ChatException(val innerException: Exception) : Exception()
}