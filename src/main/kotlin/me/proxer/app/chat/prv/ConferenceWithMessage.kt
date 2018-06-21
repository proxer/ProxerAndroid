package me.proxer.app.chat.prv

import android.arch.persistence.room.Embedded
import me.proxer.library.enums.MessageAction

/**
 * @author Ruben Gees
 */
data class ConferenceWithMessage(
    @Embedded val conference: LocalConference,
    val message: String? = null,
    val username: String? = null,
    val action: MessageAction? = null
)
