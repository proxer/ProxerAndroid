package me.proxer.app.data

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import me.proxer.app.entity.LocalUser
import me.proxer.app.entity.chat.*
import me.proxer.library.entitiy.messenger.Message
import me.proxer.library.enums.Device
import me.proxer.library.enums.MessageAction
import me.proxer.library.util.ProxerUtils
import org.jetbrains.anko.db.*
import org.threeten.bp.Instant
import java.util.*

/**
 * @author Ruben Gees
 */
class ChatDatabase(context: Context) : ManagedSQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "chat.db"
        private const val DATABASE_VERSION = 1

        private const val CONFERENCE_TABLE = "conference"
        private const val MESSAGE_TABLE = "message"

        private const val CONFERENCE_LOCAL_ID_COLUMN = "_id"
        private const val CONFERENCE_ID_COLUMN = "id"
        private const val CONFERENCE_TOPIC_COLUMN = "topic"
        private const val CONFERENCE_CUSTOM_TOPIC_COLUMN = "custom_topic"
        private const val CONFERENCE_PARTICIPANT_AMOUNT_COLUMN = "participant_amount"
        private const val CONFERENCE_IS_GROUP_COLUMN = "is_group"
        private const val CONFERENCE_LAST_MESSAGE_DATE_COLUMN = "last_message_date"
        private const val CONFERENCE_LOCAL_IS_READ_COLUMN = "is_read_local"
        private const val CONFERENCE_IS_READ_COLUMN = "is_read"
        private const val CONFERENCE_UNREAD_MESSAGE_AMOUNT_COLUMN = "unread_message_amount"
        private const val CONFERENCE_LAST_READ_MESSAGE_ID_COLUMN = "last_read_message_id"
        private const val CONFERENCE_IMAGE_COLUMN = "image"
        private const val CONFERENCE_IMAGE_TYPE_COLUMN = "image_type"
        private const val CONFERENCE_IS_LOADED_FULLY = "is_loaded_fully"

        private const val MESSAGE_LOCAL_ID_COLUMN = "_id"
        private const val MESSAGE_ID_COLUMN = "id"
        private const val MESSAGE_CONFERENCE_ID_COLUMN = "conference_id"
        private const val MESSAGE_USER_ID_COLUMN = "user_id"
        private const val MESSAGE_USER_NAME_COLUMN = "username"
        private const val MESSAGE_MESSAGE_COLUMN = "message"
        private const val MESSAGE_ACTION_COLUMN = "action"
        private const val MESSAGE_DATE_COLUMN = "date"
        private const val MESSAGE_DEVICE_COLUMN = "device"
    }

    private val conferenceParser = rowParser { localId: Long, id: String, topic: String, customTopic: String,
                                               participantAmount: Int, image: String, imageType: String, isGroup: Int,
                                               localIsRead: Int, isRead: Int, lastMessageDate: Long,
                                               unreadMessageAmount: Int, lastReadMessageId: String,
                                               isLoadedFully: Int ->

        LocalConference(localId, id, topic, customTopic, participantAmount, image, imageType, isGroup == 1,
                localIsRead == 1, isRead == 1, Date(lastMessageDate), unreadMessageAmount, lastReadMessageId,
                isLoadedFully == 1)
    }

    private val messageParser = rowParser { localId: Long, id: String, conferenceId: String, userId: String,
                                            username: String, message: String, action: String, date: Long,
                                            device: String ->

        val messageAction = ProxerUtils.toApiEnum(MessageAction::class.java, action)
                ?: throw IllegalArgumentException("Unknown message action: $action")
        val messageDevice = ProxerUtils.toApiEnum(Device::class.java, device)
                ?: throw IllegalArgumentException("Unknown device: $device")

        LocalMessage(localId, id, conferenceId, userId, username, message, messageAction, Date(date), messageDevice)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(CONFERENCE_TABLE, true,
                CONFERENCE_LOCAL_ID_COLUMN to INTEGER + PRIMARY_KEY + UNIQUE + NOT_NULL,
                CONFERENCE_ID_COLUMN to TEXT + NOT_NULL,
                CONFERENCE_TOPIC_COLUMN to TEXT + NOT_NULL,
                CONFERENCE_CUSTOM_TOPIC_COLUMN to TEXT + NOT_NULL,
                CONFERENCE_PARTICIPANT_AMOUNT_COLUMN to INTEGER + NOT_NULL,
                CONFERENCE_IMAGE_COLUMN to TEXT + NOT_NULL,
                CONFERENCE_IMAGE_TYPE_COLUMN to TEXT + NOT_NULL,
                CONFERENCE_IS_GROUP_COLUMN to INTEGER + NOT_NULL,
                CONFERENCE_LOCAL_IS_READ_COLUMN to INTEGER + NOT_NULL,
                CONFERENCE_IS_READ_COLUMN to INTEGER + NOT_NULL,
                CONFERENCE_LAST_MESSAGE_DATE_COLUMN to INTEGER + NOT_NULL,
                CONFERENCE_UNREAD_MESSAGE_AMOUNT_COLUMN to INTEGER + NOT_NULL,
                CONFERENCE_LAST_READ_MESSAGE_ID_COLUMN to TEXT + NOT_NULL,
                CONFERENCE_IS_LOADED_FULLY to INTEGER + NOT_NULL)

        db.createTable(MESSAGE_TABLE, true,
                MESSAGE_LOCAL_ID_COLUMN to INTEGER + PRIMARY_KEY + UNIQUE + NOT_NULL,
                MESSAGE_ID_COLUMN to TEXT + NOT_NULL,
                MESSAGE_CONFERENCE_ID_COLUMN to TEXT + NOT_NULL,
                MESSAGE_USER_ID_COLUMN to TEXT + NOT_NULL,
                MESSAGE_USER_NAME_COLUMN to TEXT + NOT_NULL,
                MESSAGE_MESSAGE_COLUMN to TEXT + NOT_NULL,
                MESSAGE_ACTION_COLUMN to TEXT + NOT_NULL,
                MESSAGE_DATE_COLUMN to INTEGER + NOT_NULL,
                MESSAGE_DEVICE_COLUMN to TEXT + NOT_NULL,
                FOREIGN_KEY(MESSAGE_CONFERENCE_ID_COLUMN, CONFERENCE_TABLE, CONFERENCE_ID_COLUMN))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Not needed yet.
    }

    fun getAllConferences(): List<LocalConference> {
        return use {
            this.select(CONFERENCE_TABLE)
                    .orderBy(CONFERENCE_LAST_MESSAGE_DATE_COLUMN, SqlOrderDirection.DESC)
                    .parseList(conferenceParser)
        }
    }

    fun getAllMessages(conferenceId: String): List<LocalMessage> {
        return use {
            val result = ArrayList<LocalMessage>()

            result.addAll(this.select(MESSAGE_TABLE)
                    .whereArgs("($MESSAGE_CONFERENCE_ID_COLUMN = $conferenceId) and ($MESSAGE_ID_COLUMN != -1)")
                    .orderBy(MESSAGE_ID_COLUMN, SqlOrderDirection.DESC)
                    .parseList(messageParser))

            result.addAll(0, this.select(MESSAGE_TABLE)
                    .whereArgs("($MESSAGE_CONFERENCE_ID_COLUMN = $conferenceId) and ($MESSAGE_ID_COLUMN = -1)")
                    .orderBy(MESSAGE_DATE_COLUMN, SqlOrderDirection.DESC)
                    .parseList(messageParser))

            result
        }
    }

    fun insertOrUpdateConferencesWithMessages(items: List<ConferenceAssociation>): List<LocalConferenceAssociation> {
        return use {
            val result = ArrayList<LocalConferenceAssociation>()

            transaction {
                for (container in items) {
                    result.add(LocalConferenceAssociation(
                            doInsertOrUpdateConference(this, container.conference),
                            doInsertOrUpdateMessages(this, container.messages)
                    ))
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

    fun insertMessageToSend(user: LocalUser, conferenceId: String, message: String): LocalMessage {
        return use {
            var result: LocalMessage? = null

            transaction {
                val date = Instant.now().epochSecond
                val id = this.insertOrThrow(MESSAGE_TABLE,
                        MESSAGE_ID_COLUMN to "-1",
                        MESSAGE_CONFERENCE_ID_COLUMN to conferenceId,
                        MESSAGE_USER_ID_COLUMN to user.id,
                        MESSAGE_USER_NAME_COLUMN to user.name,
                        MESSAGE_MESSAGE_COLUMN to message,
                        MESSAGE_ACTION_COLUMN to "",
                        MESSAGE_DATE_COLUMN to date,
                        MESSAGE_DEVICE_COLUMN to "mobile")

                result = LocalMessage(id, "-1", conferenceId, user.id, user.name, message, MessageAction.NONE,
                        Date(date), Device.MOBILE)
            }

            result ?: throw SQLiteException()
        }
    }

    fun getMessagesToSend(): List<LocalMessage> {
        return use {
            this.select(MESSAGE_TABLE)
                    .whereArgs("$MESSAGE_ID_COLUMN = -1")
                    .orderBy(MESSAGE_LOCAL_ID_COLUMN, SqlOrderDirection.ASC)
                    .parseList(messageParser)
        }
    }

    fun removeMessageToSend(id: Long) {
        use {
            this.delete(MESSAGE_TABLE, "$MESSAGE_LOCAL_ID_COLUMN = $id")
        }
    }

    fun clear() {
        use {
            transaction {
                this.delete(CONFERENCE_TABLE)
                this.delete(MESSAGE_TABLE)
            }
        }
    }

    fun findConference(id: String): LocalConference? {
        return use {
            this.select(CONFERENCE_TABLE)
                    .whereArgs("$CONFERENCE_ID_COLUMN = $id")
                    .parseOpt(conferenceParser)
        }
    }

    fun getConference(id: String): LocalConference {
        return findConference(id) ?: throw SQLiteException()
    }

    fun findMessage(id: String): LocalMessage? {
        return use {
            this.select(MESSAGE_TABLE)
                    .whereArgs("$MESSAGE_ID_COLUMN = $id")
                    .parseOpt(messageParser)
        }
    }

    fun getMessage(id: String): LocalMessage {
        return findMessage(id) ?: throw SQLiteException()
    }

    fun getUnreadConferences(): List<LocalConference> {
        return use {
            this.select(CONFERENCE_TABLE)
                    .whereArgs("$CONFERENCE_IS_READ_COLUMN = 0")
                    .orderBy(CONFERENCE_ID_COLUMN, SqlOrderDirection.DESC)
                    .parseList(conferenceParser)
        }
    }

    fun getUnreadMessageAmount(conferenceId: String, lastReadMessageId: String): Long {
        return use {
            DatabaseUtils.queryNumEntries(this, MESSAGE_TABLE,
                    "($MESSAGE_CONFERENCE_ID_COLUMN = $conferenceId) and ($MESSAGE_ID_COLUMN > $lastReadMessageId)")
        }
    }

    fun findOldestMessage(conferenceId: String): LocalMessage? {
        return use {
            this.select(MESSAGE_TABLE)
                    .whereArgs("($MESSAGE_CONFERENCE_ID_COLUMN = $conferenceId) and ($MESSAGE_ID_COLUMN != -1)")
                    .orderBy(MESSAGE_ID_COLUMN, SqlOrderDirection.ASC)
                    .limit(1)
                    .parseOpt(messageParser)
        }
    }

    fun getMostRecentMessage(conferenceId: String): LocalMessage? {
        return use {
            this.select(MESSAGE_TABLE)
                    .whereArgs("($MESSAGE_CONFERENCE_ID_COLUMN = $conferenceId) and ($MESSAGE_ID_COLUMN != -1)")
                    .orderBy(MESSAGE_ID_COLUMN, SqlOrderDirection.DESC)
                    .limit(1)
                    .parseOpt(messageParser)
        }
    }

    fun getMostRecentMessages(conferenceId: String, amount: Int): List<LocalMessage> {
        return use {
            this.select(MESSAGE_TABLE)
                    .whereArgs("($MESSAGE_CONFERENCE_ID_COLUMN = $conferenceId) and ($MESSAGE_ID_COLUMN != -1)")
                    .orderBy(MESSAGE_ID_COLUMN, SqlOrderDirection.DESC)
                    .limit(amount)
                    .parseList(messageParser)
        }
    }

    fun getConferencesToMarkAsRead(): List<LocalConference> {
        return use {
            this.select(CONFERENCE_TABLE)
                    .whereArgs("($CONFERENCE_LOCAL_IS_READ_COLUMN = 1) and ($CONFERENCE_IS_READ_COLUMN = 0)")
                    .parseList(conferenceParser)
        }
    }

    fun markAsRead(conferenceId: String) {
        use {
            transaction {
                this.update(CONFERENCE_TABLE, CONFERENCE_LOCAL_IS_READ_COLUMN to 1)
                        .whereArgs("$CONFERENCE_ID_COLUMN = $conferenceId")
                        .exec()
            }
        }
    }

    fun markConferenceAsFullyLoadedAndGet(conferenceId: String): LocalConference {
        return use {
            var result: LocalConference? = null

            transaction {
                this.update(CONFERENCE_TABLE, CONFERENCE_IS_LOADED_FULLY to 1)
                        .whereArgs("$CONFERENCE_ID_COLUMN = $conferenceId")
                        .exec()

                result = getConference(conferenceId)
            }

            result ?: throw SQLiteException()
        }
    }

    private fun doInsertOrUpdateConference(db: SQLiteDatabase, conference: MetaConference): LocalConference {
        val id = conference.conference.id
        val insertionValues = generateInsertionValues(conference)
        val updated = db.update(CONFERENCE_TABLE, *insertionValues)
                .whereArgs("$CONFERENCE_ID_COLUMN = $id")
                .exec() > 0

        return when (updated) {
            true -> getConference(id)
            false -> conference.toLocalConference(db.insertOrThrow(CONFERENCE_TABLE, *insertionValues))
        }
    }

    private fun doInsertOrUpdateMessages(db: SQLiteDatabase, items: Collection<Message>): List<LocalMessage> {
        return items.map {
            val insertionValues = generateInsertionValues(it)
            val updated = db.update(MESSAGE_TABLE, *insertionValues)
                    .whereArgs("$MESSAGE_ID_COLUMN = ${it.id}")
                    .exec() > 0

            when (updated) {
                true -> getMessage(it.id)
                false -> it.toLocalMessage(db.insertOrThrow(MESSAGE_TABLE, *insertionValues))
            }
        }
    }

    private fun generateInsertionValues(item: Message): Array<Pair<String, Any?>> {
        return arrayOf(MESSAGE_ID_COLUMN to item.id,
                MESSAGE_CONFERENCE_ID_COLUMN to item.conferenceId,
                MESSAGE_USER_ID_COLUMN to item.userId,
                MESSAGE_USER_NAME_COLUMN to item.username,
                MESSAGE_MESSAGE_COLUMN to item.message,
                MESSAGE_ACTION_COLUMN to ProxerUtils.getApiEnumName(item.action),
                MESSAGE_DATE_COLUMN to item.date.time,
                MESSAGE_DEVICE_COLUMN to ProxerUtils.getApiEnumName(item.device))
    }

    private fun generateInsertionValues(item: MetaConference): Array<Pair<String, Any?>> {
        val (conference, isLoadedFully) = item

        return arrayOf(CONFERENCE_ID_COLUMN to conference.id,
                CONFERENCE_TOPIC_COLUMN to conference.topic,
                CONFERENCE_CUSTOM_TOPIC_COLUMN to conference.customTopic,
                CONFERENCE_PARTICIPANT_AMOUNT_COLUMN to conference.participantAmount,
                CONFERENCE_IMAGE_COLUMN to conference.image,
                CONFERENCE_IMAGE_TYPE_COLUMN to conference.imageType,
                CONFERENCE_IS_GROUP_COLUMN to if (conference.isGroup) 1 else 0,
                CONFERENCE_LOCAL_IS_READ_COLUMN to if (conference.isRead) 1 else 0,
                CONFERENCE_IS_READ_COLUMN to if (conference.isRead) 1 else 0,
                CONFERENCE_LAST_MESSAGE_DATE_COLUMN to conference.date.time,
                CONFERENCE_UNREAD_MESSAGE_AMOUNT_COLUMN to conference.unreadMessageAmount,
                CONFERENCE_LAST_READ_MESSAGE_ID_COLUMN to conference.lastReadMessageId,
                CONFERENCE_IS_LOADED_FULLY to if (isLoadedFully) 1 else 0)
    }
}
