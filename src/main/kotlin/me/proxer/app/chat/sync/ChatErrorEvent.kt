package me.proxer.app.chat.sync

import me.proxer.app.exception.ChatException

/**
 * @author Ruben Gees
 */
data class ChatErrorEvent(val error: ChatException)
