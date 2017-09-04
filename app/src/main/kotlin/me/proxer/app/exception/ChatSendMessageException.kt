package me.proxer.app.exception

class ChatSendMessageException(innerError: Throwable, val id: Long) : ChatException(innerError)
