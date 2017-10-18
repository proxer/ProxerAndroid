package me.proxer.app.exception

/**
 * @author Ruben Gees
 */
open class ChatException(val innerError: Throwable) : Exception()
