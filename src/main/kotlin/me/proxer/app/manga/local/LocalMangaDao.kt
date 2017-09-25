package me.proxer.app.manga.local

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import me.proxer.library.enums.Language

/**
 * @author Ruben Gees
 */
@Dao
abstract class LocalMangaDao {

    @Transaction
    open fun clear() {
        clearPages()
        clearChapters()
        clearEntries()
    }

    @Transaction
    open fun deleteChapterAndEntryIfEmpty(chapter: LocalMangaChapter) {
        deletePagesOfChapter(chapter.id)
        deleteChapter(chapter.id)

        if (countChaptersForEntry(chapter.entryId) <= 0) {
            deleteEntry(chapter.entryId)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertEntry(entry: LocalEntryCore)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertChapterAndPages(chapter: LocalMangaChapter, pages: List<LocalMangaPage>)

    @Query("SELECT * FROM entries WHERE id = :entryId LIMIT 1")
    abstract fun findEntry(entryId: Long): LocalEntryCore?

    @Query("SELECT * FROM chapters WHERE entryId = :entryId AND episode = :episode AND language = :language LIMIT 1")
    abstract fun findChapter(entryId: Long, episode: Int, language: Language): LocalMangaChapter?

    @Query("SELECT * FROM entries")
    abstract fun getEntries(): List<LocalEntryCore>

    @Query("SELECT * FROM chapters WHERE entryId = :entryId ORDER BY episode ASC")
    abstract fun getChaptersForEntry(entryId: Long): List<LocalMangaChapter>

    @Query("SELECT * FROM pages WHERE chapterId = :chapterId")
    abstract fun getPagesForChapter(chapterId: Long): List<LocalMangaPage>

    @Query("SELECT COUNT(*) FROM entries WHERE id = :id")
    abstract fun countEntries(id: Long): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE entryId = :entryId")
    abstract fun countChaptersForEntry(entryId: Long): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE entryId = :entryId AND episode = :episode AND language = :language")
    abstract fun countSpecificChaptersForEntry(entryId: Long, episode: Int, language: Language): Int

    @Query("DELETE FROM entries WHERE id = :entryId")
    abstract fun deleteEntry(entryId: Long)

    @Query("DELETE FROM chapters WHERE id = :chapterId")
    abstract fun deleteChapter(chapterId: Long)

    @Query("DELETE FROM pages WHERE chapterId = :chapterId")
    abstract fun deletePagesOfChapter(chapterId: Long)

    @Query("DELETE FROM entries")
    abstract fun clearEntries()

    @Query("DELETE FROM chapters")
    abstract fun clearChapters()

    @Query("DELETE FROM pages")
    abstract fun clearPages()
}
