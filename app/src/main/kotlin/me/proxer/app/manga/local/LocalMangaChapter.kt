package me.proxer.app.manga.local

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import me.proxer.library.entity.manga.Chapter
import me.proxer.library.entity.manga.Page
import me.proxer.library.enums.Language
import java.util.*

/**
 * @author Ruben Gees
 */
@Entity(tableName = "chapters", foreignKeys = [(ForeignKey(
        entity = LocalEntryCore::class,
        parentColumns = [("id")],
        childColumns = [("entryId")]
))], indices = [(Index(value = "entryId"))])
data class LocalMangaChapter(@PrimaryKey(autoGenerate = true) val id: Long = 0, val episode: Int,
                             val language: Language, val entryId: Long, val title: String, val uploaderId: String,
                             val uploaderName: String, val date: Date, val scanGroupId: String?,
                             val scanGroupName: String?, val server: String) {

    fun toNonLocalChapter(pages: List<Page>) = Chapter(id.toString(), entryId.toString(), title, uploaderId,
            uploaderName, date, scanGroupId, scanGroupName, server, pages)
}
