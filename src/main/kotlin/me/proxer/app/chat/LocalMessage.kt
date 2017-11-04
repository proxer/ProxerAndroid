package me.proxer.app.chat

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import me.proxer.library.entity.messenger.Message
import me.proxer.library.enums.Device
import me.proxer.library.enums.MessageAction
import java.util.Date

/**
 * @author Ruben Gees
 */
@Entity(tableName = "messages", foreignKeys = [(ForeignKey(
        entity = LocalConference::class,
        parentColumns = ["id"],
        childColumns = ["conferenceId"]
))], indices = [(Index("conferenceId"))])
data class LocalMessage(@PrimaryKey(autoGenerate = true) val id: Long, val conferenceId: Long, val userId: String,
                        val username: String, val message: String, val action: MessageAction, val date: Date,
                        val device: Device) {

    fun toNonLocalMessage() = Message(id.toString(), conferenceId.toString(), userId, username, message, action, date,
            device)
}
