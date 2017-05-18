package me.proxer.app.event

import me.proxer.app.job.ChatJob.ChatException

/**
 * @author Ruben Gees
 */
class ChatErrorEvent(val error: ChatException)