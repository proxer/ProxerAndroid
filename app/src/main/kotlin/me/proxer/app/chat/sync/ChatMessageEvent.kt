package me.proxer.app.chat.sync

import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.LocalMessage

/**
 * @author Ruben Gees
 */
class ChatMessageEvent(val data: Pair<LocalConference, List<LocalMessage>>)
