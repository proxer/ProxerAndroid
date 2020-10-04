package me.proxer.app.chat.prv.sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import io.reactivex.Maybe
import me.proxer.app.auth.LocalUser
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.library.enums.Device
import me.proxer.library.enums.MessageAction
import org.koin.core.KoinComponent
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
@Dao
abstract class MessengerDao : KoinComponent {

    @Transaction
    open fun insertMessageToSend(user: LocalUser, text: String, conferenceId: Long): LocalMessage {
        val message = LocalMessage(
            calculateNextMessageToSendId(),
            conferenceId,
            user.id,
            user.name,
            text,
            MessageAction.NONE,
            Instant.now(),
            Device.MOBILE
        )

        insertMessage(message)
        markConferenceAsRead(conferenceId)

        return message
    }

    @Transaction
    open fun clear() {
        clearMessages()
        clearConferences()
    }

    open fun getMessagesLiveDataForConference(conferenceId: Long): MediatorLiveData<List<LocalMessage>> {
        val sentLiveData = getSentMessagesLiveDataForConference(conferenceId)
        val unsentLiveData = getUnsentMessagesLiveDataForConference(conferenceId)

        return MediatorLiveData<List<LocalMessage>>().apply {
            addSource(sentLiveData) {
                value = (unsentLiveData.value ?: emptyList()) + (it ?: emptyList())
            }

            addSource(unsentLiveData) {
                // Prevent duplicate update when detecting sent messages.
                // If the amount of unsent messages is lower than before, these messages have been sent.
                // Without this check, both sources would update (racy) and potentially cause weird visual effects.
                if (it?.size ?: 0 >= value?.takeWhile { message -> message.id < 0 }?.size ?: 0) {
                    value = (it ?: emptyList()) + (sentLiveData.value ?: emptyList())
                }
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertConferences(conference: List<LocalConference>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertMessages(messages: List<LocalMessage>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insertMessage(message: LocalMessage): Long

    @Query("SELECT * FROM conferences ORDER BY date DESC")
    abstract fun getConferences(): List<LocalConference>

    @Query("SELECT * FROM conferences ORDER BY date DESC LIMIT :amount")
    abstract fun getMostRecentConferences(amount: Int): List<LocalConference>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
            SELECT * FROM conferences
            LEFT JOIN (
                SELECT id AS messageId, conferenceId, userId, message AS messageText, username,
                       `action` as messageAction from messages
                GROUP BY conferenceId
                HAVING MAX(date)
                ORDER BY date DESC, id
            ) AS messages
            ON conferences.id = messages.conferenceId
            WHERE topic LIKE '%' || :searchQuery || '%'
            ORDER BY date DESC
            """
    )
    abstract fun getConferencesLiveData(searchQuery: String): LiveData<List<ConferenceWithMessage>?>

    @Query("SELECT * FROM conferences WHERE id = :id LIMIT 1")
    abstract fun getConferenceLiveData(id: Long): LiveData<LocalConference?>

    @Query("SELECT * FROM conferences WHERE id = :id LIMIT 1")
    abstract fun getConferenceMaybe(id: Long): Maybe<LocalConference>

    @Query("SELECT * FROM conferences WHERE localIsRead = 0 AND isRead = 0 ORDER BY id DESC")
    abstract fun getUnreadConferences(): List<LocalConference>

    @Query("SELECT * FROM conferences WHERE localIsRead != 0 AND isRead = 0")
    abstract fun getConferencesToMarkAsRead(): List<LocalConference>

    @Query("SELECT * FROM conferences WHERE id = :id LIMIT 1")
    abstract fun getConference(id: Long): LocalConference

    @Query("SELECT * FROM conferences WHERE id = :id LIMIT 1")
    abstract fun findConference(id: Long): LocalConference?

    @Query("SELECT * FROM conferences WHERE topic = :username LIMIT 1")
    abstract fun findConferenceForUser(username: String): LocalConference?

    @Query("SELECT * FROM (SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id DESC) ")
    abstract fun getSentMessagesLiveDataForConference(conferenceId: Long): LiveData<List<LocalMessage>>

    @Query("SELECT * FROM (SELECT * FROM messages WHERE conferenceId = :conferenceId AND id < 0 ORDER BY id ASC)")
    abstract fun getUnsentMessagesLiveDataForConference(conferenceId: Long): LiveData<List<LocalMessage>>

    @Query("SELECT COUNT(*) FROM messages WHERE conferenceId = :conferenceId AND id = :lastReadMessageId")
    abstract fun getUnreadMessageAmountForConference(conferenceId: Long, lastReadMessageId: Long): Int

    @Query("SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id DESC LIMIT :amount")
    abstract fun getMostRecentMessagesForConference(conferenceId: Long, amount: Int): List<LocalMessage>

    @Query("SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id DESC LIMIT 1")
    abstract fun findMostRecentMessageForConference(conferenceId: Long): LocalMessage?

    @Query("SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id ASC LIMIT 1")
    abstract fun findOldestMessageForConference(conferenceId: Long): LocalMessage?

    @Query("SELECT MIN(id) FROM messages")
    abstract fun findLowestMessageId(): Long?

    @Query("SELECT * FROM messages WHERE id < 0 ORDER BY id DESC")
    abstract fun getMessagesToSend(): List<LocalMessage>

    @Query("DELETE FROM messages WHERE id = :messageId")
    abstract fun deleteMessageToSend(messageId: Long)

    @Query("UPDATE conferences SET localIsRead = 1 WHERE id = :conferenceId")
    abstract fun markConferenceAsRead(conferenceId: Long)

    @Query("UPDATE conferences SET isFullyLoaded = 1 WHERE id = :conferenceId")
    abstract fun markConferenceAsFullyLoaded(conferenceId: Long)

    @Query("DELETE FROM messages WHERE conferenceId = :conferenceId")
    abstract fun deleteMessagesByConferenceId(conferenceId: String)

    @Query("DELETE FROM conferences WHERE id = :id")
    abstract fun deleteConferenceById(id: String)

    @Query("DELETE FROM conferences")
    abstract fun clearConferences()

    @Query("DELETE FROM messages")
    abstract fun clearMessages()

    private fun calculateNextMessageToSendId(): Long {
        val candidate = findLowestMessageId() ?: -1L

        return when (candidate < 0) {
            true -> candidate - 1
            false -> -1L
        }
    }
}
