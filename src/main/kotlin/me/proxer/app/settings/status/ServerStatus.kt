package me.proxer.app.settings.status

/**
 * @author Ruben Gees
 */
data class ServerStatus(val name: String, val number: Int, val type: ServerType, val online: Boolean)
