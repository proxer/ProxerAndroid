package me.proxer.app.chat.sync

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.LocalMessage

/**
 * @author Ruben Gees
 */
@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertConferences(conference: List<LocalConference>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<LocalMessage>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertMessage(message: LocalMessage): Long

    @Query("SELECT * FROM conferences ORDER BY date DESC")
    fun getConferencesLiveData(): LiveData<List<LocalConference>?>

    @Query("SELECT * FROM conferences WHERE id = :id LIMIT 1")
    fun getConferenceLiveData(id: Long): LiveData<LocalConference?>

    @Query("SELECT * FROM conferences WHERE localIsRead = 0 AND isRead = 0 ORDER BY id DESC")
    fun getUnreadConferences(): List<LocalConference>

    @Query("SELECT * FROM conferences WHERE localIsRead != 0 AND isRead = 0")
    fun getConferencesToMarkAsRead(): List<LocalConference>

    @Query("SELECT * FROM conferences WHERE id = :id LIMIT 1")
    fun findConference(id: Long): LocalConference?

    @Query("SELECT * FROM conferences WHERE topic = :username LIMIT 1")
    fun findConferenceForUser(username: String): LocalConference?

    @Query("SELECT * FROM (SELECT * FROM messages WHERE conferenceId = :conferenceId AND id < 0 ORDER BY id ASC) " +
            "UNION ALL " +
            "SELECT * FROM (SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id DESC)")
    fun getMessagesLiveDataForConference(conferenceId: Long): LiveData<List<LocalMessage>>

    @Query("SELECT COUNT(*) FROM messages WHERE conferenceId = :conferenceId AND id = :lastReadMessageId")
    fun getUnreadMessageAmountForConference(conferenceId: Long, lastReadMessageId: Long): Int

    @Query("SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id DESC LIMIT :amount")
    fun getMostRecentMessagesForConference(conferenceId: Long, amount: Int): List<LocalMessage>

    @Query("SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id DESC LIMIT 1")
    fun findMostRecentMessageForConference(conferenceId: Long): LocalMessage?

    @Query("SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id ASC LIMIT 1")
    fun findOldestMessageForConference(conferenceId: Long): LocalMessage?

    @Query("SELECT MIN(id) FROM messages")
    fun findLowestMessageId(): Long?

    @Query("SELECT * FROM messages WHERE id < 0 ORDER BY id DESC")
    fun getMessagesToSend(): List<LocalMessage>

    @Query("DELETE FROM messages WHERE id = :messageId")
    fun deleteMessageToSend(messageId: Long)

    @Query("UPDATE conferences SET localIsRead = 1 WHERE id = :conferenceId")
    fun markConferenceAsRead(conferenceId: Long)

    @Query("UPDATE conferences SET isFullyLoaded = 1 WHERE id = :conferenceId")
    fun markConferenceAsFullyLoaded(conferenceId: Long)

    @Query("DELETE FROM conferences")
    fun clearConferences()

    @Query("DELETE FROM messages")
    fun clearMessages()
}
