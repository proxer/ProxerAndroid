package me.proxer.app.exception

/**
 * @author Ruben Gees
 */
class ChatSynchronizationException(innerError: Throwable) : ChatException(innerError)
