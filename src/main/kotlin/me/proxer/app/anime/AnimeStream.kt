package me.proxer.app.anime

import java.util.Date

/**
 * @author Ruben Gees
 */
data class AnimeStream(
    val id: String,
    val hoster: String,
    val hosterName: String,
    val image: String,
    val uploaderId: String,
    val uploaderName: String,
    val date: Date,
    val translatorGroupId: String?,
    val translatorGroupName: String?,
    val isOfficial: Boolean,
    val isSupported: Boolean,
    val isInternalPlayerOnly: Boolean
)
