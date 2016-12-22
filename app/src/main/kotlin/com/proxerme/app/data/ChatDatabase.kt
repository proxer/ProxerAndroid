package com.proxerme.app.data

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.entitiy.toLocalConference
import com.proxerme.app.entitiy.toLocalMessage
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

        const val COLUMN_CONFERENCE_LOCAL_ID = "_id"
        const val COLUMN_CONFERENCE_ID = "id"
        const val COLUMN_CONFERENCE_TOPIC = "topic"
        const val COLUMN_CONFERENCE_CUSTOM_TOPIC = "customTopic"
        const val COLUMN_CONFERENCE_PARTICIPANT_AMOUNT = "participantAmount"
        const val COLUMN_CONFERENCE_IS_GROUP = "isGroup"
        const val COLUMN_CONFERENCE_LAST_MESSAGE_TIME = "lastMessageTime"
        const val COLUMN_CONFERENCE_LOCAL_IS_READ = "isReadLocal"
        const val COLUMN_CONFERENCE_IS_READ = "isRead"
        const val COLUMN_CONFERENCE_UNREAD_MESSAGE_AMOUNT = "unreadMessageAmount"
        const val COLUMN_CONFERENCE_LAST_READ_MESSAGE_ID = "lastReadMessageId"
        const val COLUMN_CONFERENCE_IMAGE_ID = "imageId"
        const val COLUMN_CONFERENCE_IMAGE_TYPE = "imageType"

        const val COLUMN_MESSAGE_LOCAL_ID = "_id"
        const val COLUMN_MESSAGE_ID = "id"
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

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(TABLE_CONFERENCE, true,
                COLUMN_CONFERENCE_LOCAL_ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                COLUMN_CONFERENCE_ID to TEXT + NOT_NULL,
                COLUMN_CONFERENCE_TOPIC to TEXT + NOT_NULL,
                COLUMN_CONFERENCE_CUSTOM_TOPIC to TEXT + NOT_NULL,
                COLUMN_CONFERENCE_PARTICIPANT_AMOUNT to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_IMAGE_TYPE to TEXT + NOT_NULL,
                COLUMN_CONFERENCE_IMAGE_ID to TEXT + NOT_NULL,
                COLUMN_CONFERENCE_IS_GROUP to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_LOCAL_IS_READ to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_IS_READ to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_LAST_MESSAGE_TIME to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_UNREAD_MESSAGE_AMOUNT to INTEGER + NOT_NULL,
                COLUMN_CONFERENCE_LAST_READ_MESSAGE_ID to TEXT + NOT_NULL)
        db.createTable(TABLE_MESSAGE, true,
                COLUMN_MESSAGE_LOCAL_ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                COLUMN_MESSAGE_ID to TEXT + NOT_NULL,
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

    fun getConferences(): List<LocalConference> {
        return use {
            this.select(ChatDatabase.TABLE_CONFERENCE)
                    .orderBy(ChatDatabase.COLUMN_CONFERENCE_LAST_MESSAGE_TIME,
                            SqlOrderDirection.DESC)
                    .parseList(conferenceParser)
        }
    }

    fun getMessages(conferenceId: String): List<LocalMessage> {
        return use {
            val result = ArrayList<LocalMessage>()

            result.addAll(this.select(ChatDatabase.TABLE_MESSAGE)
                    .where("($COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId) and " +
                            "($COLUMN_MESSAGE_ID != -1)")
                    .orderBy(COLUMN_MESSAGE_ID, SqlOrderDirection.DESC)
                    .parseList(messageParser))

            result.addAll(0, this.select(ChatDatabase.TABLE_MESSAGE)
                    .where("($COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId) and " +
                            "($COLUMN_MESSAGE_ID = -1)")
                    .orderBy(COLUMN_MESSAGE_TIME, SqlOrderDirection.DESC)
                    .parseList(messageParser))

            result
        }
    }

    fun insertOrUpdate(conferenceMap: Map<Conference, List<Message>>):
            Map<LocalConference, List<LocalMessage>> {
        return use {
            val result = LinkedHashMap<LocalConference, List<LocalMessage>>()

            transaction {
                for ((conference, messages) in conferenceMap) {
                    result.put(doInsertOrUpdateConference(this, conference),
                            doInsertOrUpdateMessages(this, messages))
                }
            }

            result
        }
    }

    fun insertOrUpdateMessages(items: Collection<Message>): List<LocalMessage> {
        return use {
            var result: List<LocalMessage>? = null

            transaction {
                result = doInsertOrUpdateMessages(this, items)
            }

            result ?: throw SQLiteException()
        }
    }

    fun insertMessageToSend(user: User, conferenceId: String, message: String): LocalMessage {
        return use {
            var result: LocalMessage? = null

            transaction {
                val id = this.insertOrThrow(ChatDatabase.TABLE_MESSAGE,
                        COLUMN_MESSAGE_ID to "-1",
                        COLUMN_MESSAGE_CONFERENCE_ID to conferenceId,
                        COLUMN_MESSAGE_USER_ID to user.id,
                        COLUMN_MESSAGE_USER_NAME to user.username,
                        COLUMN_MESSAGE_MESSAGE to message,
                        COLUMN_MESSAGE_ACTION to "",
                        COLUMN_MESSAGE_TIME to DateTime.now().millis / 1000L,
                        COLUMN_MESSAGE_DEVICE to "mobile")

                result = LocalMessage(id, "-1", conferenceId, user.id, user.username, message, "",
                        DateTime.now().millis / 1000L, "mobile")
            }

            result ?: throw SQLiteException()
        }
    }

    fun getMessagesToSend(): List<LocalMessage> {
        return use {
            this.select(ChatDatabase.TABLE_MESSAGE)
                    .where("$COLUMN_MESSAGE_ID = -1")
                    .orderBy(COLUMN_MESSAGE_LOCAL_ID, SqlOrderDirection.ASC)
                    .parseList(messageParser)
        }
    }

    fun removeMessageToSend(id: Long) {
        use {
            this.delete(TABLE_MESSAGE, "$COLUMN_MESSAGE_LOCAL_ID = $id")
        }
    }

    fun clear() {
        use {
            transaction {
                this.delete(TABLE_CONFERENCE)
                this.delete(TABLE_MESSAGE)
            }
        }
    }

    fun getConference(id: String): LocalConference? {
        return use {
            this.select(TABLE_CONFERENCE)
                    .where("$COLUMN_CONFERENCE_ID = $id")
                    .parseOpt(conferenceParser)
        }
    }

    fun getMessage(id: String): LocalMessage? {
        return use {
            this.select(TABLE_MESSAGE)
                    .where("$COLUMN_MESSAGE_ID = $id")
                    .parseOpt(messageParser)
        }
    }

    fun getUnreadConferences(): List<LocalConference> {
        return use {
            this.select(TABLE_CONFERENCE)
                    .where("$COLUMN_CONFERENCE_IS_READ = 0")
                    .orderBy(COLUMN_CONFERENCE_ID, SqlOrderDirection.DESC)
                    .parseList(conferenceParser)
        }
    }

    fun getUnreadMessageAmount(conferenceId: String, lastReadMessageId: String): Long {
        return use {
            DatabaseUtils.queryNumEntries(this, TABLE_MESSAGE,
                    "($COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId) and " +
                            "($COLUMN_MESSAGE_ID > $lastReadMessageId)")
        }
    }

    private fun doInsertOrUpdateConference(db: SQLiteDatabase, item: Conference): LocalConference {
        val insertionValues = generateInsertionValues(item)
        val updated = db.update(TABLE_CONFERENCE, *insertionValues)
                .where("$COLUMN_CONFERENCE_ID = ${item.id}").exec() > 0

        return if (updated) {
            getConference(item.id) ?: throw SQLiteException("Could not find conference with " +
                    "id ${item.id}")
        } else {
            item.toLocalConference(db.insertOrThrow(TABLE_CONFERENCE, *insertionValues))
        }
    }

    private fun doInsertOrUpdateConferences(db: SQLiteDatabase, items: Collection<Conference>):
            List<LocalConference> {
        return items.map {
            val insertionValues = generateInsertionValues(it)
            val updated = db.update(TABLE_CONFERENCE, *insertionValues)
                    .where("$COLUMN_CONFERENCE_ID = ${it.id}").exec() > 0

            if (updated) {
                getConference(it.id) ?: throw SQLiteException("Could not find conference with " +
                        "id ${it.id}")
            } else {
                it.toLocalConference(db.insertOrThrow(TABLE_CONFERENCE, *insertionValues))
            }
        }
    }

    private fun doInsertOrUpdateMessages(db: SQLiteDatabase, items: Collection<Message>):
            List<LocalMessage> {
        return items.map {
            val insertionValues = generateInsertionValues(it)
            val updated = db.update(TABLE_MESSAGE, *insertionValues)
                    .where("$COLUMN_MESSAGE_ID = ${it.id}")
                    .exec() > 0

            if (updated) {
                getMessage(it.id) ?: throw SQLiteException("Could not find message with i" +
                        "d ${it.id}")
            } else {
                it.toLocalMessage(db.insertOrThrow(TABLE_MESSAGE, *insertionValues))
            }
        }
    }

    fun getOldestMessage(conferenceId: String): LocalMessage? {
        return use {
            this.select(TABLE_MESSAGE)
                    .where("($COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId) and " +
                            "($COLUMN_MESSAGE_ID != -1)")
                    .orderBy(COLUMN_MESSAGE_ID, SqlOrderDirection.ASC)
                    .limit(1)
                    .parseOpt(messageParser)
        }
    }

    fun getMostRecentMessage(conferenceId: String): LocalMessage? {
        return use {
            this.select(TABLE_MESSAGE)
                    .where("($COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId) and " +
                            "($COLUMN_MESSAGE_ID != -1)")
                    .orderBy(COLUMN_MESSAGE_ID, SqlOrderDirection.DESC)
                    .limit(1)
                    .parseOpt(messageParser)
        }
    }

    fun getMostRecentMessages(conferenceId: String, amount: Int): List<LocalMessage> {
        return use {
            this.select(TABLE_MESSAGE)
                    .where("($COLUMN_MESSAGE_CONFERENCE_ID = $conferenceId) and " +
                            "($COLUMN_MESSAGE_ID != -1)")
                    .orderBy(COLUMN_MESSAGE_ID, SqlOrderDirection.DESC)
                    .limit(amount)
                    .parseList(messageParser)
        }
    }

    fun getChat(username: String): LocalConference? {
        return use {
            this.select(TABLE_CONFERENCE)
                    .where("($COLUMN_CONFERENCE_TOPIC = '$username') and " +
                            "($COLUMN_CONFERENCE_IS_GROUP = 0)")
                    .parseOpt(conferenceParser)
        }
    }

    fun getConferencesToMark(): List<LocalConference> {
        return use {
            this.select(TABLE_CONFERENCE)
                    .where("($COLUMN_CONFERENCE_LOCAL_IS_READ = 1) and " +
                            "($COLUMN_CONFERENCE_IS_READ = 0)")
                    .parseList(conferenceParser)
        }
    }

    private fun generateInsertionValues(message: Message): Array<Pair<String, Any?>> {
        return arrayOf(COLUMN_MESSAGE_ID to message.id,
                COLUMN_MESSAGE_CONFERENCE_ID to message.conferenceId,
                COLUMN_MESSAGE_USER_ID to message.userId,
                COLUMN_MESSAGE_USER_NAME to message.username,
                COLUMN_MESSAGE_MESSAGE to message.message,
                COLUMN_MESSAGE_ACTION to message.action,
                COLUMN_MESSAGE_TIME to message.time,
                COLUMN_MESSAGE_DEVICE to message.device)
    }

    private fun generateInsertionValues(conference: Conference): Array<Pair<String, Any?>> {
        return arrayOf(COLUMN_CONFERENCE_ID to conference.id,
                COLUMN_CONFERENCE_TOPIC to conference.topic,
                COLUMN_CONFERENCE_CUSTOM_TOPIC to conference.customTopic,
                COLUMN_CONFERENCE_PARTICIPANT_AMOUNT to conference.participantAmount,
                COLUMN_CONFERENCE_IMAGE_TYPE to conference.imageType,
                COLUMN_CONFERENCE_IMAGE_ID to conference.imageId,
                COLUMN_CONFERENCE_IS_GROUP to if (conference.isGroup) 1 else 0,
                COLUMN_CONFERENCE_LOCAL_IS_READ to if (conference.isRead) 1 else 0,
                COLUMN_CONFERENCE_IS_READ to if (conference.isRead) 1 else 0,
                COLUMN_CONFERENCE_LAST_MESSAGE_TIME to conference.time,
                COLUMN_CONFERENCE_UNREAD_MESSAGE_AMOUNT to conference.unreadMessageAmount,
                COLUMN_CONFERENCE_LAST_READ_MESSAGE_ID to conference.lastReadMessageId)
    }

    fun markAsRead(conferenceId: String) {
        use {
            transaction {
                this.update(TABLE_CONFERENCE, COLUMN_CONFERENCE_LOCAL_IS_READ to 1)
                        .where("$COLUMN_CONFERENCE_ID = $conferenceId")
                        .exec()
            }
        }
    }
}

val Context.chatDatabase: ChatDatabase
    get() = ChatDatabase.getInstance(applicationContext)