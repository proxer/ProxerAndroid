package me.proxer.app.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class LocalUser
    (
    @Json(name = "token") val token: String,
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "image") val image: String
)
