package me.proxer.app.entity.manga

import me.proxer.library.entitiy.manga.Chapter
import me.proxer.library.entitiy.manga.Page
import me.proxer.library.enums.Language
import java.util.*

/**
 * @author Ruben Gees
 */
data class LocalMangaChapter(val localId: Long, val id: String, val episode: Int, val language: Language,
                             val entryId: String, val title: String, val uploaderId: String, val uploaderName: String,
                             val date: Date, val scanGroupId: String?, val scanGroupName: String?, val server: String) {

    fun toNonLocalChapter(pages: List<Page>): Chapter {
        return Chapter(id, entryId, title, uploaderId, uploaderName, date, scanGroupId, scanGroupName,
                server, pages)
    }
}