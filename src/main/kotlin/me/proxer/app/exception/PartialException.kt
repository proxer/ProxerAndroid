package me.proxer.app.exception

/**
 * @author Ruben Gees
 */
class PartialException(val innerError: Throwable, val partialData: Any?) : Exception(innerError)
