package com.proxerme.app.event

import com.proxerme.app.entitiy.LocalMessage

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ChatMessagesEvent(@Suppress("unused") val conferenceId: String = "", val messages: List<LocalMessage>)