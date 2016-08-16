package com.proxerme.app.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.event.ChatEvent
import com.proxerme.app.event.ConferencesEvent
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.ServiceHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.manager.UserManager
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.messenger.entity.Conference
import com.proxerme.library.connection.messenger.entity.Message
import com.proxerme.library.connection.messenger.request.ConferencesRequest
import com.proxerme.library.connection.messenger.request.MessagesRequest
import com.proxerme.library.connection.messenger.request.SendMessageRequest
import org.greenrobot.eventbus.EventBus
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ChatService : IntentService("ChatService") {

    companion object {
        private const val ACTION_SYNCHRONIZE = "com.proxerme.app.service.action.SYNCHRONIZE"
        private const val ACTION_LOAD_CONFERENCES =
                "com.proxerme.app.service.action.LOAD_CONFERENCES"
        private const val ACTION_LOAD_MESSAGES = "com.proxerme.app.service.action.LOAD_MESSAGES"

        private const val EXTRA_CONFERENCE_ID = "conferenceId"

        private const val CONFERENCES_ON_PAGE = 48
        private const val MESSAGES_ON_PAGE = 30

        var isSynchronizing = false
            private set
        var isLoadingConferences = false
            private set
        private val isLoadingMessagesMap = HashMap<String, Boolean>()

        fun isLoadingMessages(conferenceId: String): Boolean {
            return isLoadingMessagesMap.getOrElse(conferenceId, { false })
        }

        fun synchronize(context: Context) {
            ServiceHelper.cancelChatRetrieval(context)

            context.startService(Intent(context, ChatService::class.java)
                    .apply { action = ACTION_SYNCHRONIZE })
        }

        fun loadMoreConferences(context: Context) {
            context.startService(Intent(context, ChatService::class.java)
                    .apply { action = ACTION_LOAD_CONFERENCES })
        }

        fun loadMoreMessages(context: Context, conferenceId: String) {
            context.startService(Intent(context, ChatService::class.java)
                    .apply {
                        action = ACTION_LOAD_MESSAGES

                        putExtra(EXTRA_CONFERENCE_ID, conferenceId)
                    })
        }

        fun reschedule(context: Context) {
            when (SectionManager.currentSection) {
                SectionManager.Section.CHAT -> StorageHelper.chatInterval = 3
                SectionManager.Section.CONFERENCES -> StorageHelper.chatInterval = 10
                else -> StorageHelper.incrementChatInterval()
            }

            ServiceHelper.retrieveChatLater(context)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_SYNCHRONIZE -> isSynchronizing = true
            ACTION_LOAD_CONFERENCES -> isLoadingConferences = true
            ACTION_LOAD_MESSAGES -> {
                isLoadingMessagesMap.put(intent.getStringExtra(EXTRA_CONFERENCE_ID), true)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onHandleIntent(intent: Intent) {
        if (UserManager.user == null) {
            chatDatabase.clear()
        } else {
            try {
                UserManager.reLoginSync()

                when (intent.action) {
                    ACTION_SYNCHRONIZE -> doSynchronize()
                    ACTION_LOAD_CONFERENCES -> doLoadConferences()
                    ACTION_LOAD_MESSAGES -> {
                        doLoadMessages(intent.getStringExtra(EXTRA_CONFERENCE_ID))
                    }
                    else -> return
                }
            } catch(exception: ProxerException) {

            } catch (exception: Exception) {

            }

            when (intent.action) {
                ACTION_SYNCHRONIZE -> {
                    isSynchronizing = false

                    reschedule(this@ChatService)
                }
                ACTION_LOAD_CONFERENCES -> isLoadingConferences = false
                ACTION_LOAD_MESSAGES -> {
                    isLoadingMessagesMap.remove(intent.getStringExtra(EXTRA_CONFERENCE_ID))
                }
                else -> return
            }
        }
    }

    private fun doSynchronize() {
        sendMessages()
        val changedConferences = fetchConferences()

        if (changedConferences.size > 0) {
            val newMessages = fetchMessages(changedConferences)

            chatDatabase.insertOrUpdate(changedConferences, newMessages)

            when (SectionManager.currentSection) {
                SectionManager.Section.CONFERENCES -> EventBus.getDefault().post(ConferencesEvent())
                SectionManager.Section.CHAT -> EventBus.getDefault().post(ChatEvent())
                else -> {
                    showNotification()
                }
            }
        }
    }

    private fun doLoadConferences() {
        val pageToRetrieve = chatDatabase.getConferenceAmount() / CONFERENCES_ON_PAGE

        chatDatabase.insertOrUpdateConferences(ConferencesRequest(pageToRetrieve.toInt())
                .executeSynchronized().item.toList())

        EventBus.getDefault().post(ConferencesEvent())
    }

    private fun doLoadMessages(conferenceId: String) {
        val idToLoadFrom = chatDatabase.getOldestMessage(conferenceId)?.id ?: "0"

        chatDatabase.insertOrUpdateMessages(MessagesRequest(conferenceId, idToLoadFrom)
                .executeSynchronized().item.toList())

        EventBus.getDefault().post(ChatEvent())
    }

    private fun sendMessages() {
        chatDatabase.getMessagesToSend().forEach {
            try {
                val result = SendMessageRequest(it.conferenceId, it.message).executeSynchronized()

                if (result.item != null) {
                    throw MessageSendingException(result.item)
                }

                chatDatabase.removeMessageToSend(it.localId)
            } catch(exception: ProxerException) {

            }
        }
    }

    private fun fetchConferences(): Collection<Conference> {
        val changedConferences = HashSet<Conference>()
        var page = 0

        while (true) {
            val fetchedConferences = ConferencesRequest(page).executeSynchronized().item

            for (conference in fetchedConferences) {
                if (conference != chatDatabase.getConference(conference.id)) {
                    changedConferences.add(conference)
                } else {
                    break
                }
            }

            if (fetchedConferences.size < CONFERENCES_ON_PAGE) {
                StorageHelper.conferenceListEndReached = true

                break
            }

            if (changedConferences.size / (page + 1) < CONFERENCES_ON_PAGE) {
                break
            } else {
                page++
            }
        }

        return changedConferences
    }

    private fun fetchMessages(changedConferences: Collection<Conference>): Collection<Message> {
        val newMessages = LinkedList<Message>()

        for (conference in changedConferences) {
            var mostRecentMessage = chatDatabase.getMostRecentMessage(conference.id)
            var existingUnreadMessageAmount = chatDatabase.getUnreadMessageAmount(conference.id,
                    conference.lastReadMessageId)
            var nextId = "0"

            if (mostRecentMessage == null) {
                while (existingUnreadMessageAmount < conference.unreadMessageAmount) {
                    val fetchedMessages = MessagesRequest(conference.id, nextId)
                            .executeSynchronized().item

                    newMessages.addAll(fetchedMessages)

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
                    val fetchedMessages = MessagesRequest(conference.id, nextId)
                            .executeSynchronized().item

                    newMessages.addAll(fetchedMessages)

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
        }

        return newMessages
    }

    private fun showNotification() {
        val unreadMap = HashMap<LocalConference, List<Message>>()

        chatDatabase.getUnreadConferences().forEach {
            val unreadMessages = chatDatabase.getMostRecentMessages(it.id, it.unreadMessageAmount)

            if (unreadMessages.size > 0) {
                unreadMap.put(it, unreadMessages)
            }
        }

        NotificationHelper.showChatNotification(this, unreadMap)
    }

    class MessageSendingException(message: String?) : Exception(message)
}