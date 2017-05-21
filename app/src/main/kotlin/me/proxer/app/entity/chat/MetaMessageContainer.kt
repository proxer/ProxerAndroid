package me.proxer.app.entity.chat

import me.proxer.library.entitiy.messenger.Message

/**
 * @author Ruben Gees
 */
class MetaMessageContainer(val messages: List<Message>, val isFullyLoaded: Boolean = false)
