package me.proxer.app.exception

open class ChatException(val innerError: Throwable) : Exception()
