package me.proxer.app.auth

/**
 * @author Ruben Gees
 */
data class LocalUser(val token: String, val id: String, val name: String, val image: String)
