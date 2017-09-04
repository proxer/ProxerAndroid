package me.proxer.app.exception

class ChatMessageException(innerError: Throwable) : ChatException(innerError)
