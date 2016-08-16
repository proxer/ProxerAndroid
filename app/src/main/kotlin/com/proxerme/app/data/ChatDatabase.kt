package com.proxerme.app.data

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import com.proxerme.library.connection.messenger.entity.Conference
import com.proxerme.library.connection.messenger.entity.Message
import com.proxerme.library.connection.user.entitiy.User
import org.jetbrains.anko.db.*
import org.joda.time.DateTime
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ChatDatabase(context: Context) :
        ManagedSQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "ChatDatabase"
        const val DATABASE_VERSION = 1

        const val TABLE_CONFERENCE = "Conference"
        const val TABLE_MESSAGE = "Message"
        const val TABLE_MESSAGE_TO_SEND = "MessageToSend"

        const val COLUMN_CONFERENCE_ID = "_id"
        const val COLUMN_CONFERENCE_TOPIC = "topic"
        const val COLUMN_CONFERENCE_CUSTOM_TOPIC = "customTopic"
        const val COLUMN_CONFERENCE_PARTICIPANT_AMOUNT = "participantAmount"
        const val COLUMN_CONFERENCE_IS_GROUP = "isGroup"
        const val COLUMN_CONFERENCE_LAST_MESSAGE_TIME = "lastMessageTime"
        const val COLUMN_CONFERENCE_IS_READ = "isRead"
        const val COLUMN_CONFERENCE_UNREAD_MESSAGE_AMOUNT = "unreadMessageAmount"
        const val COLUMN_CONFERENCE_LAST_READ_MESSAGE_ID = "lastReadMessageId"
        const val COLUMN_CONFERENCE_IMAGE_ID = "imageId"
        const val COLUMN_CONFERENCE_IMAGE_TYPE = "imageType"

        const val COLUMN_MESSAGE_ID = "_id"
        const val COLUMN_MESSAGE_CONFERENCE_ID = "conferenceId"
        const val COLUMN_MESSAGE_USER_ID = "user_id"
        const val COLUMN_MESSAGE_USER_NAME = "username"
        const val COLUMN_MESSAGE_MESSAGE = "message"
        const val COLUMN_MESSAGE_ACTION = "action"
        const val COLUMN_MESSAGE_TIME = "time"
        const val COLUMN_MESSAGE_DEVICE = "device"

        private var instance: ChatDatabase? = null

        @Synchronized
        fun getInstance(ctx: Context): ChatDatabase {
            if (instance == null) {
                instance = ChatDatabase(ctx.applicationContext)
            }

            return instance!!
        }
    }

    val conferenceParser = ConferenceParser()
    val messageParser = MessageParser()
    val messageToSendParser = MessageToSendParser()

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(TABLE_CONFERENCE, true,
                COLUMN_CONFERENCE_ID to INTEGER + PRIMARY_KEY,
                COLUMN_CONFERENCE_TOPIC to TEXT + NOT_NULL,
                COLUMN_CONFERENCE_CUSTOM_TOPIC to TEXT + NOT_NULL,
                COLUMN_CONFERENCE_PARTICIPANT_AMOUNT to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_IMAGE_TYPE to TEXT + NOT_NULL,
                COLUMN_CONFERENCE_IMAGE_ID to TEXT + NOT_NULL,
                COLUMN_CONFERENCE_IS_GROUP to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_IS_READ to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_LAST_MESSAGE_TIME to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_UNREAD_MESSAGE_AMOUNT to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_LAST_READ_MESSAGE_ID to TEXT + NOT_NULL)
        db.createTable(TABLE_MESSAGE, true,
                COLUMN_MESSAGE_ID to INTEGER + PRIMARY_KEY,
                COLUMN_MESSAGE_CONFERENCE_ID to INTEGER + NOT_NULL,
                COLUMN_MESSAGE_USER_ID to INTEGER + NOT_NULL,
                COLUMN_MESSAGE_USER_NAME to TEXT + NOT_NULL,
                COLUMN_MESSAGE_MESSAGE to TEXT + NOT_NULL,
                COLUMN_MESSAGE_ACTION to TEXT + NOT_NULL,
                COLUMN_MESSAGE_TIME to INTEGER + NOT_NULL,
                COLUMN_MESSAGE_DEVICE to TEXT + NOT_NULL)
        db.createTable(TABLE_MESSAGE_TO_SEND, true,
                COLUMN_MESSAGE_ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                COLUMN_MESSAGE_CONFERENCE_ID to INTEGER + NOT_NULL,
                COLUMN_MESSAGE_USER_ID to INTEGER + NOT_NULL,
                COLUMN_MESSAGE_USER_NAME to TEXT + NOT_NULL,
                COLUMN_MESSAGE_MESSAGE to TEXT + NOT_NULL,
                COLUMN_MESSAGE_ACTION to TEXT + NOT_NULL,
                COLUMN_MESSAGE_TIME to INTEGER + NOT_NULL,
                COLUMN_MESSAGE_DEVICE to TEXT + NOT_NULL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    fun getConferences(): List<Conference> {
        return use {
            this.select(ChatDatabase.TABLE_CONFERENCE)
                    .orderBy(ChatDatabase.COLUMN_CONFERENCE_LAST_MESSAGE_TIME,
                            SqlOrderDirection.DESC)
                    .parseList(conferenceParser)
        }
    }

    fun getMessages(conferenceId: String): List<Message> {
        return use {
            val result = ArrayList(this.select(ChatDatabase.TABLE_MESSAGE)
                    .where("$COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId")
                    .parseList(messageParser))

            result.addAll(this.select(ChatDatabase.TABLE_MESSAGE_TO_SEND)
                    .where("$COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId")
                    .parseList(messageToSendParser))

            result.sortByDescending { it.time }

            result
        }
    }

    fun insertOrUpdate(conferences: Collection<Conference>, messages: Collection<Message>) {
        use {
            transaction {
                doInsertOrUpdateConferences(this, conferences)
                doInsertOrUpdateMessages(this, messages)
            }
        }
    }

    fun insertOrUpdateConferences(items: Collection<Conference>) {
        use {
            transaction {
                doInsertOrUpdateConferences(this, items)
            }
        }
    }

    fun insertOrUpdateMessages(items: Collection<Message>) {
        use {
            transaction {
                doInsertOrUpdateMessages(this, items)
            }
        }
    }

    fun insertMessageToSend(user: User, conference: Conference, message: String) {
        use {
            transaction {
                this.replaceOrThrow(ChatDatabase.TABLE_MESSAGE_TO_SEND,
                        COLUMN_MESSAGE_CONFERENCE_ID to conference.id,
                        COLUMN_MESSAGE_USER_ID to user.id,
                        COLUMN_MESSAGE_USER_NAME to user.username,
                        COLUMN_MESSAGE_MESSAGE to message,
                        COLUMN_MESSAGE_ACTION to "",
                        COLUMN_MESSAGE_TIME to DateTime.now().millis / 1000L,
                        COLUMN_MESSAGE_DEVICE to "mobile")
            }
        }
    }

    fun getMessagesToSend(): List<Message> {
        return use {
            this.select(ChatDatabase.TABLE_MESSAGE_TO_SEND)
                    .orderBy(COLUMN_MESSAGE_TIME, SqlOrderDirection.ASC)
                    .parseList(messageParser)

        }
    }

    fun removeMessageToSend(id: Long) {
        use {
            this.delete(TABLE_MESSAGE_TO_SEND, "$COLUMN_MESSAGE_ID = $id")
        }
    }

    fun clear() {
        use {
            transaction {
                this.delete(TABLE_CONFERENCE)
                this.delete(TABLE_MESSAGE)
                this.delete(TABLE_MESSAGE_TO_SEND)
            }
        }
    }

    fun getConference(id: String): Conference? {
        return use {
            this.select(TABLE_CONFERENCE)
                    .where("$COLUMN_CONFERENCE_ID = $id")
                    .parseOpt(conferenceParser)
        }
    }

    fun getUnreadConferences(): List<Conference> {
        return use {
            this.select(TABLE_CONFERENCE)
                    .where("$COLUMN_CONFERENCE_IS_READ = 0")
                    .orderBy(COLUMN_CONFERENCE_ID, SqlOrderDirection.DESC)
                    .parseList(conferenceParser)
        }
    }

    fun getConferenceAmount(): Long {
        return use {
            DatabaseUtils.queryNumEntries(this, TABLE_CONFERENCE)
        }
    }

    fun getUnreadMessageAmount(conferenceId: String, lastReadMessageId: String): Long {
        return use {
            DatabaseUtils.queryNumEntries(this, TABLE_MESSAGE,
                    "($COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId) and " +
                            "($COLUMN_MESSAGE_ID > $lastReadMessageId)")
        }
    }

    private fun doInsertOrUpdateConferences(db: SQLiteDatabase, items: Collection<Conference>) {
        items.forEach {
            db.replaceOrThrow(ChatDatabase.TABLE_CONFERENCE,
                    COLUMN_CONFERENCE_ID to it.id,
                    COLUMN_CONFERENCE_TOPIC to it.topic,
                    COLUMN_CONFERENCE_CUSTOM_TOPIC to it.customTopic,
                    COLUMN_CONFERENCE_PARTICIPANT_AMOUNT to it.participantAmount,
                    COLUMN_CONFERENCE_IMAGE_TYPE to it.imageType,
                    COLUMN_CONFERENCE_IMAGE_ID to it.imageId,
                    COLUMN_CONFERENCE_IS_GROUP to if (it.isGroup) 1 else 0,
                    COLUMN_CONFERENCE_IS_READ to if (it.isRead) 1 else 0,
                    COLUMN_CONFERENCE_LAST_MESSAGE_TIME to it.time,
                    COLUMN_CONFERENCE_UNREAD_MESSAGE_AMOUNT to it.unreadMessageAmount,
                    COLUMN_CONFERENCE_LAST_READ_MESSAGE_ID to it.lastReadMessageId)
        }
    }

    private fun doInsertOrUpdateMessages(db: SQLiteDatabase, items: Collection<Message>) {
        items.forEach {
            db.replaceOrThrow(ChatDatabase.TABLE_MESSAGE,
                    COLUMN_MESSAGE_ID to it.id,
                    COLUMN_MESSAGE_CONFERENCE_ID to it.conferenceId,
                    COLUMN_MESSAGE_USER_ID to it.userId,
                    COLUMN_MESSAGE_USER_NAME to it.username,
                    COLUMN_MESSAGE_MESSAGE to it.message,
                    COLUMN_MESSAGE_ACTION to it.action,
                    COLUMN_MESSAGE_TIME to it.time,
                    COLUMN_MESSAGE_DEVICE to it.device)
        }
    }

    fun getOldestMessage(conferenceId: String): Message? {
        return use {
            this.select(TABLE_MESSAGE)
                    .where("$COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId")
                    .orderBy(COLUMN_MESSAGE_ID, SqlOrderDirection.ASC)
                    .limit(1)
                    .parseOpt(messageParser)
        }
    }

    fun getMostRecentMessage(conferenceId: String): Message? {
        return use {
            this.select(TABLE_MESSAGE)
                    .where("$COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId")
                    .orderBy(COLUMN_MESSAGE_ID, SqlOrderDirection.DESC)
                    .limit(1)
                    .parseOpt(messageParser)
        }
    }

    fun getMostRecentMessages(conferenceId: String, amount: Int): List<Message> {
        return use {
            this.select(TABLE_MESSAGE)
                    .where("$COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId")
                    .orderBy(COLUMN_MESSAGE_ID, SqlOrderDirection.DESC)
                    .limit(amount)
                    .parseList(messageParser)
        }
    }
}

val Context.chatDatabase: ChatDatabase
    get() = ChatDatabase.getInstance(applicationContext)