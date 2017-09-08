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
interface LocalMangaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntry(entry: LocalEntryCore)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChapterAndPages(chapter: LocalMangaChapter, pages: List<LocalMangaPage>)

    @Query("SELECT * FROM entries WHERE id = :entryId LIMIT 1")
    fun findEntry(entryId: Long): LocalEntryCore?

    @Query("SELECT * FROM chapters WHERE entryId = :entryId AND episode = :episode AND language = :language LIMIT 1")
    fun findChapter(entryId: Long, episode: Int, language: Language): LocalMangaChapter?

    @Query("SELECT * FROM entries")
    fun getEntries(): List<LocalEntryCore>

    @Query("SELECT * FROM chapters WHERE entryId = :entryId ORDER BY episode ASC")
    fun getChaptersForEntry(entryId: Long): List<LocalMangaChapter>

    @Query("SELECT * FROM pages WHERE chapterId = :chapterId")
    fun getPagesForChapter(chapterId: Long): List<LocalMangaPage>

    @Query("SELECT COUNT(*) FROM entries WHERE id = :id")
    fun countEntries(id: Long): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE entryId = :entryId")
    fun countChaptersForEntry(entryId: Long): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE entryId = :entryId AND episode = :episode AND language = :language")
    fun countSpecificChaptersForEntry(entryId: Long, episode: Int, language: Language): Int

    @Query("DELETE FROM entries WHERE id = :entryId")
    fun deleteEntry(entryId: Long)

    @Query("DELETE FROM chapters WHERE id = :chapterId")
    fun deleteChapter(chapterId: Long)

    @Query("DELETE FROM pages WHERE chapterId = :chapterId")
    fun deletePagesOfChapter(chapterId: Long)

    @Query("DELETE FROM entries")
    fun clearEntries()

    @Query("DELETE FROM chapters")
    fun clearChapters()

    @Query("DELETE FROM pages")
    fun clearPages()
}
