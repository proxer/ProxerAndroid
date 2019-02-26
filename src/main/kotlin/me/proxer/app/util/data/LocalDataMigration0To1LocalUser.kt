package me.proxer.app.util.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class for temporary storage of the user when migrating older versions of the app.
 * This *needs* to be a class on its own, since Moshi does not seem to find it otherwise.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
internal data class LocalDataMigration0To1LocalUser(
    @Json(name = "a") val token: String,
    @Json(name = "b") val id: String,
    @Json(name = "c") val name: String,
    @Json(name = "d") val image: String
)
