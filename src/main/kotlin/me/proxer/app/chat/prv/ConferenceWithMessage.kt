package me.proxer.app.chat.prv

import android.arch.persistence.room.Embedded
import me.proxer.library.enums.MessageAction

/**
 * @author Ruben Gees
 */
data class ConferenceWithMessage(
    @Embedded val conference: LocalConference,
    @Embedded val message: SimpleLocalMessage?
) {

    data class SimpleLocalMessage(
        val messageId: Long,
        val messageText: String,
        val userId: String,
        val username: String,
        val messageAction: MessageAction
    )
}
