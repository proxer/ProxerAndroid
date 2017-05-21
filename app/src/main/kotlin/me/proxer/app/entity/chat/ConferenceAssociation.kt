package me.proxer.app.entity.chat

import me.proxer.library.entitiy.messenger.Message

/**
 * @author Ruben Gees
 */
class ConferenceAssociation(val conference: MetaConference, val messages: List<Message>)
