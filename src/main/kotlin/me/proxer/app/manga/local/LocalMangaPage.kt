package me.proxer.app.manga.local

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import me.proxer.library.entity.manga.Page

/**
 * @author Ruben Gees
 */
@Entity(tableName = "pages", foreignKeys = arrayOf(ForeignKey(
        entity = LocalMangaChapter::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("chapterId")
)), indices = arrayOf(Index(value = "chapterId")))
data class LocalMangaPage(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String,
                          val height: Int, val width: Int, val chapterId: Long) {

    fun toNonLocalPage() = Page(name, height, width)
}
