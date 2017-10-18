package me.proxer.app.exception

/**
 * @author Ruben Gees
 */
class ChatMessageException(innerError: Throwable) : ChatException(innerError)
