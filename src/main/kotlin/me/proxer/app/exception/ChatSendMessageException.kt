package me.proxer.app.exception

/**
 * @author Ruben Gees
 */
class ChatSendMessageException(innerError: Throwable, val id: Long) : ChatException(innerError)
