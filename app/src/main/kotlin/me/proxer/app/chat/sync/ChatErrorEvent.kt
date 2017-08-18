package me.proxer.app.chat.sync

import me.proxer.app.chat.sync.ChatJob.ChatException

/**
 * @author Ruben Gees
 */
class ChatErrorEvent(val error: ChatException)
