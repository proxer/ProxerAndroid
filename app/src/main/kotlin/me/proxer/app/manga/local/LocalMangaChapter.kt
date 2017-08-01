package me.proxer.app.manga.local

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import me.proxer.library.entitiy.manga.Chapter
import me.proxer.library.entitiy.manga.Page
import me.proxer.library.enums.Language
import java.util.*

/**
 * @author Ruben Gees
 */
@Entity(tableName = "chapters", foreignKeys = arrayOf(ForeignKey(
        entity = LocalEntryCore::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("entryId")
)), indices = arrayOf(Index(value = "entryId")))
data class LocalMangaChapter(@PrimaryKey(autoGenerate = true) val id: Long = 0, val episode: Int,
                             val language: Language, val entryId: String, val title: String, val uploaderId: String,
                             val uploaderName: String, val date: Date, val scanGroupId: String?,
                             val scanGroupName: String?, val server: String) {

    fun toNonLocalChapter(pages: List<Page>) = Chapter(id.toString(), entryId, title, uploaderId, uploaderName, date,
            scanGroupId, scanGroupName, server, pages)
}
