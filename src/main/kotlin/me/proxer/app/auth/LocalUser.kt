package me.proxer.app.auth

import com.squareup.moshi.JsonClass

/**
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class LocalUser(val token: String, val id: String, val name: String, val image: String)
