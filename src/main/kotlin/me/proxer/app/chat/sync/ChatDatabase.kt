package me.proxer.app.chat.sync

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.LocalMessage
import me.proxer.app.util.converter.RoomConverters
import me.proxer.app.util.converter.RoomJavaConverters
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.enums.Device
import me.proxer.library.enums.MessageAction
import java.util.Date

/**
 * @author Ruben Gees
 */
@Database(entities = [(LocalConference::class), (LocalMessage::class)], version = 1, exportSchema = false)
@TypeConverters(RoomConverters::class, RoomJavaConverters::class)
abstract class ChatDatabase : RoomDatabase() {

    companion object {
        private var nextMessageToSendId = 0L
    }

    abstract fun dao(): ChatDao

    fun insertMessageToSend(text: String, conferenceId: Long): LocalMessage = dao().let { dao ->
        var result: LocalMessage? = null

        runInTransaction {
            if (nextMessageToSendId >= 0) {
                calculateNextMessageToSendId()
            }

            val user = StorageHelper.user ?: throw IllegalStateException("User cannot be null")
            val message = LocalMessage(nextMessageToSendId, conferenceId, user.id, user.name, text,
                    MessageAction.NONE, Date(), Device.MOBILE)

            dao.insertMessage(message)
            dao.markConferenceAsRead(conferenceId)

            nextMessageToSendId--

            result = message
        }

        result ?: throw IllegalStateException("Message cannot be null")
    }

    fun clear() = dao().let { dao ->
        runInTransaction {
            dao.clearMessages()
            dao.clearConferences()
        }
    }

    private fun calculateNextMessageToSendId() {
        val candidate = dao().findLowestMessageId() ?: -1L

        nextMessageToSendId = when (candidate < 0) {
            true -> candidate
            false -> -1L
        }
    }
}
