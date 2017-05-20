package me.proxer.app.event

import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.entity.chat.LocalMessage

/**
 * @author Ruben Gees
 */
class ChatMessageEvent(val conference: LocalConference, val messages: List<LocalMessage>)