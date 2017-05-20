package me.proxer.app.event

import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.entity.chat.LocalMessage

/**
 * @author Ruben Gees
 */
class ChatSynchronizationEvent(val data: Map<LocalConference, List<LocalMessage>>)