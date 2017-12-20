package me.proxer.app.manga

import me.proxer.library.entity.manga.Chapter

/**
 * @author Ruben Gees
 */
data class MangaChapterInfo(val chapter: Chapter, val name: String, val episodeAmount: Int, val isLocal: Boolean)
