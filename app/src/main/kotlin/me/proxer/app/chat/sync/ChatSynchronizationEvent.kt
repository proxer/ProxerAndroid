package me.proxer.app.chat.sync

import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.LocalMessage

/**
 * @author Ruben Gees
 */
class ChatSynchronizationEvent(val dataMap: Map<LocalConference, List<LocalMessage>>)
