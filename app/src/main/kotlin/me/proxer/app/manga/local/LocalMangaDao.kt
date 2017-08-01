package me.proxer.app.manga.local

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import me.proxer.library.enums.Language

/**
 * @author Ruben Gees
 */
@Dao
abstract class LocalMangaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertEntry(entry: LocalEntryCore)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertChapter(chapter: LocalMangaChapter, pages: List<LocalMangaPage>)

    @Query("SELECT * FROM entries WHERE id = :entryId")
    abstract fun findEntry(entryId: Long): LocalEntryCore?

    @Query("SELECT * FROM chapters WHERE entryId = :entryId AND episode = :episode AND language = :language")
    abstract fun findChapter(entryId: Long, episode: Int, language: Language): LocalMangaChapter?

    @Query("SELECT * FROM pages WHERE chapterId = :chapterId")
    abstract fun getPages(chapterId: Long): List<LocalMangaPage>

    @Query("SELECT COUNT(*) FROM entries WHERE id = :id")
    abstract fun countEntries(id: Long): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE entryId = :entryId AND episode = :episode AND language = :language")
    abstract fun countChaptersForEntry(entryId: Long, episode: Int, language: Language): Int
}