package com.proxerme.app.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.event.ChatMessagesEvent
import com.proxerme.app.event.ChatSynchronizationEvent
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.ServiceHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.helper.StorageHelper.DEFAULT_CHAT_INTERVAL
import com.proxerme.app.helper.StorageHelper.LOW_CHAT_INTERVAL
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.manager.SectionManager.Section.CHAT
import com.proxerme.app.manager.SectionManager.Section.CONFERENCES
import com.proxerme.app.util.ProxerConnectionWrapper
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.messenger.entity.Conference
import com.proxerme.library.connection.messenger.entity.Message
import com.proxerme.library.connection.messenger.request.ConferencesRequest
import com.proxerme.library.connection.messenger.request.MessagesRequest
import com.proxerme.library.connection.messenger.request.ModifyConferenceRequest
import com.proxerme.library.connection.messenger.request.SendMessageRequest
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.intentFor
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ChatService : IntentService("ChatService") {

    companion object {
        private const val ACTION_SYNCHRONIZE = "com.proxerme.app.service.action.SYNCHRONIZE"
        private const val ACTION_LOAD_MESSAGES = "com.proxerme.app.service.action.LOAD_MESSAGES"

        private const val EXTRA_CONFERENCE_ID = "conferenceId"

        const val CONFERENCES_ON_PAGE = 48
        const val MESSAGES_ON_PAGE = 30

        var isSynchronizationQueued = false
            private set

        var isSynchronizing = false
            private set

        private val isLoadingMessagesMap = HashMap<String, Boolean>()

        fun isLoadingMessages(conferenceId: String): Boolean {
            return isLoadingMessagesMap.getOrElse(conferenceId, { false })
        }

        fun synchronize(context: Context) {
            if (!isSynchronizationQueued) {
                isSynchronizationQueued = true

                context.startService(context.intentFor<ChatService>().setAction(ACTION_SYNCHRONIZE))
            }
        }

        fun loadMoreMessages(context: Context, conferenceId: String) {
            if (!isLoadingMessages(conferenceId)) {
                context.startService(context.intentFor<ChatService>(EXTRA_CONFERENCE_ID to conferenceId)
                        .setAction(ACTION_LOAD_MESSAGES))
            }
        }

        fun reschedule(context: Context, changes: Boolean = false) {
            when (changes) {
                true -> StorageHelper.chatInterval = LOW_CHAT_INTERVAL
                false -> when {
                    SectionManager.isSectionResumed(CHAT) -> {
                        StorageHelper.chatInterval = LOW_CHAT_INTERVAL
                    }
                    SectionManager.isSectionResumed(CONFERENCES) -> {
                        StorageHelper.chatInterval = DEFAULT_CHAT_INTERVAL
                    }
                    else -> StorageHelper.incrementChatInterval()
                }
            }

            ServiceHelper.retrieveChatLater(context)
        }
    }

    override fun onHandleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SYNCHRONIZE -> {
                isSynchronizationQueued = false
                isSynchronizing = true
            }
            ACTION_LOAD_MESSAGES -> {
                isLoadingMessagesMap.put(intent.getStringExtra(EXTRA_CONFERENCE_ID), true)
            }
        }

        if (StorageHelper.user == null) {
            chatDatabase.clear()
            StorageHelper.conferenceListEndReached = false
            StorageHelper.resetConferenceReachedEndMap()

            isSynchronizing = false
        } else {
            when (intent.action) {
                ACTION_SYNCHRONIZE -> {
                    val changes = try {
                        doSynchronize()
                    } catch (exception: Exception) {
                        EventBus.getDefault().post(exception)

                        false
                    }

                    isSynchronizing = false

                    reschedule(this@ChatService, changes)
                }
                ACTION_LOAD_MESSAGES -> {
                    try {
                        doLoadMessages(intent.getStringExtra(EXTRA_CONFERENCE_ID))
                    } catch (exception: Exception) {
                        EventBus.getDefault().post(exception)
                    }

                    isLoadingMessagesMap.remove(intent.getStringExtra(EXTRA_CONFERENCE_ID))
                }
            }
        }
    }

    private fun doSynchronize(): Boolean {
        sendMessages()
        markConferencesAsRead()

        val insertedMap = chatDatabase.insertOrUpdate(fetchConferences().associate {
            it to fetchMessages(it).asReversed()
        })

        EventBus.getDefault().post(ChatSynchronizationEvent(insertedMap))

        if (!SectionManager.isSectionResumed(CONFERENCES) && !SectionManager.isSectionResumed(CHAT)
                && insertedMap.isNotEmpty()) {
            showNotification(insertedMap.keys)
        }

        return insertedMap.isNotEmpty()
    }

    private fun doLoadMessages(conferenceId: String) {
        return try {
            val idToLoadFrom = chatDatabase.getOldestMessage(conferenceId)?.id ?: "0"
            val newMessages = ProxerConnectionWrapper.execSync(MessagesRequest(conferenceId, idToLoadFrom)
                    .withMarkAsRead(false)).toList()

            val insertedMessages = chatDatabase.insertOrUpdateMessages(newMessages)

            if (newMessages.size < MESSAGES_ON_PAGE) {
                StorageHelper.setConferenceReachedEnd(conferenceId)
            }

            EventBus.getDefault().post(ChatMessagesEvent(conferenceId,
                    insertedMessages.asReversed()))
        } catch(exception: Exception) {
            throw LoadMoreMessagesException(exception, conferenceId)
        }
    }

    private fun sendMessages() {
        chatDatabase.getMessagesToSend().forEach {
            try {
                val result = ProxerConnectionWrapper.execSync(SendMessageRequest(it.conferenceId, it.message))

                chatDatabase.removeMessageToSend(it.localId)
                chatDatabase.markAsRead(it.conferenceId)

                if (result != null) {
                    throw SendMessageException(ProxerException(ProxerException.PROXER, result),
                            it.conferenceId)
                }
            } catch(exception: Exception) {
                throw SendMessageException(exception, it.conferenceId)
            }
        }
    }

    private fun markConferencesAsRead() {
        chatDatabase.getConferencesToMark().forEach {
            ProxerConnectionWrapper.execSync(ModifyConferenceRequest(it.id, ModifyConferenceRequest.READ))
        }
    }

    private fun fetchConferences(): Collection<Conference> {
        try {
            val changedConferences = LinkedHashSet<Conference>()
            var page = 0

            while (true) {
                val fetchedConferences = ProxerConnectionWrapper.execSync(ConferencesRequest(page))

                changedConferences += fetchedConferences.filter {
                    it != chatDatabase.getConference(it.id)?.toNonLocalConference()
                }

                if (changedConferences.size / (page + 1) < CONFERENCES_ON_PAGE) {
                    break
                } else {
                    page++
                }
            }

            StorageHelper.conferenceListEndReached = true

            return changedConferences
        } catch (exception: Exception) {
            throw FetchConferencesException(exception)
        }
    }

    private fun fetchMessages(conference: Conference): List<Message> {
        val newMessages = ArrayList<Message>()

        try {
            var mostRecentMessage: Message? = chatDatabase.getMostRecentMessage(conference.id)
            var existingUnreadMessageAmount = chatDatabase.getUnreadMessageAmount(conference.id,
                    conference.lastReadMessageId)
            var nextId = "0"

            if (mostRecentMessage == null) {
                while (existingUnreadMessageAmount < conference.unreadMessageAmount) {
                    val fetchedMessages = ProxerConnectionWrapper.execSync(
                            MessagesRequest(conference.id, nextId).withMarkAsRead(false))

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
                while (mostRecentMessage!!.time < conference.time ||
                        existingUnreadMessageAmount < conference.unreadMessageAmount) {
                    val fetchedMessages = ProxerConnectionWrapper.execSync(
                            MessagesRequest(conference.id, nextId).withMarkAsRead(false))

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
            throw FetchMessagesException(exception, conference.id)
        }

        return newMessages
    }

    private fun showNotification(changedConferences: Collection<LocalConference>) {
        val unreadMap = chatDatabase.getUnreadConferences()
                .plus(changedConferences)
                .distinct()
                .associate {
                    it to chatDatabase.getMostRecentMessages(it.id, it.unreadMessageAmount).reversed()
                }

        NotificationHelper.showChatNotification(this, unreadMap)
    }

    open class ChatException(val innerException: Exception) : Exception()
    class LoadMoreMessagesException(innerException: Exception, val conferenceId: String) :
            ChatException(innerException)

    class SendMessageException(innerException: Exception, val conferenceId: String) :
            ChatException(innerException)

    class FetchMessagesException(innerException: Exception, val conferenceId: String) :
            ChatException(innerException)

    class FetchConferencesException(innerException: Exception) :
            ChatException(innerException)
}
