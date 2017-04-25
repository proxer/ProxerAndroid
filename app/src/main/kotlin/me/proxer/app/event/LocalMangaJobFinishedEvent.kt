package me.proxer.app.event

import me.proxer.library.enums.Language

/**
 * @author Ruben Gees
 */
class LocalMangaJobFinishedEvent(val entryId: String, val episode: Int, val language: Language)
