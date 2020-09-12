package me.proxer.app.chat.prv

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.proxer.app.ui.view.bbcode.toSimpleBBTree
import me.proxer.app.util.extension.toDate
import me.proxer.library.entity.messenger.Message
import me.proxer.library.enums.Device
import me.proxer.library.enums.MessageAction
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = LocalConference::class,
            parentColumns = ["id"],
            childColumns = ["conferenceId"]
        )
    ],
    indices = [Index("conferenceId")]
)
data class LocalMessage(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val conferenceId: Long,
    val userId: String,
    val username: String,
    val message: String,
    val action: MessageAction,
    val date: Instant,
    val device: Device
) {

    @Transient
    val styledMessage = message.toSimpleBBTree()

    fun toNonLocalMessage() = Message(
        id.toString(),
        conferenceId.toString(),
        userId,
        username,
        message,
        action,
        date.toDate(),
        device
    )
}
