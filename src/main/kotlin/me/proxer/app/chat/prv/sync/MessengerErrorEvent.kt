package me.proxer.app.chat.prv.sync

import me.proxer.app.exception.ChatException

/**
 * @author Ruben Gees
 */
data class MessengerErrorEvent(val error: ChatException)
