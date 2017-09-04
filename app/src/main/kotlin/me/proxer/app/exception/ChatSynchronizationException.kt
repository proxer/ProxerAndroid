package me.proxer.app.exception

class ChatSynchronizationException(innerError: Throwable) : ChatException(innerError)
