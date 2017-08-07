package me.proxer.app.manga.local

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import me.proxer.app.util.RoomConverters
import me.proxer.app.util.RoomJavaConverters

/**
 * @author Ruben Gees
 */
@Database(entities = arrayOf(LocalEntryCore::class, LocalMangaChapter::class, LocalMangaPage::class), version = 1,
        exportSchema = false)
@TypeConverters(RoomConverters::class, RoomJavaConverters::class)
abstract class LocalMangaDatabase : RoomDatabase() {
    abstract fun dao(): LocalMangaDao

    fun deleteChapterAndEntryIfEmpty(chapter: LocalMangaChapter) = dao().let { dao ->
        runInTransaction {
            dao.deletePagesOfChapter(chapter.id)
            dao.deleteChapter(chapter.id)

            if (dao.countChaptersForEntry(chapter.entryId) <= 0) {
                dao.deleteEntry(chapter.entryId)
            }
        }
    }

    fun clear() = dao().let { dao ->
        runInTransaction {
            dao.clearPages()
            dao.clearChapters()
            dao.clearEntries()
        }
    }
}
