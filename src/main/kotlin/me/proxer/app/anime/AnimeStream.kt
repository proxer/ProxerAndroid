package me.proxer.app.anime

import me.proxer.app.anime.resolver.StreamResolutionResult
import org.threeten.bp.Instant

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
    val date: Instant,
    val translatorGroupId: String?,
    val translatorGroupName: String?,
    val isOfficial: Boolean,
    val isPublic: Boolean,
    val isSupported: Boolean,
    val resolutionResult: StreamResolutionResult?
)
