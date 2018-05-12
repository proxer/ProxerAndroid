package me.proxer.app.chat.prv

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.text.SpannableString
import me.proxer.app.ui.view.bbcode.BBParser
import me.proxer.app.util.extension.linkify
import me.proxer.library.entity.messenger.Message
import me.proxer.library.enums.Device
import me.proxer.library.enums.MessageAction
import java.util.*

/**
 * @author Ruben Gees
 */
@Entity(tableName = "messages", foreignKeys = [(ForeignKey(
    entity = LocalConference::class,
    parentColumns = ["id"],
    childColumns = ["conferenceId"]
))], indices = [(Index("conferenceId"))])
data class LocalMessage(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val conferenceId: Long,
    val userId: String,
    val username: String,
    val message: String,
    val action: MessageAction,
    val date: Date,
    val device: Device
) {

    @Transient
    val styledMessage = when (action) {
        MessageAction.NONE -> BBParser.parseTextOnly(message).linkify()
        else -> SpannableString("")
    }

    fun toNonLocalMessage() = Message(id.toString(), conferenceId.toString(), userId, username, message, action, date,
        device)
}
