package me.proxer.app.manga.local

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import me.proxer.app.util.converter.RoomConverters

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
@Database(entities = [(LocalEntryCore::class), (LocalMangaChapter::class), (LocalMangaPage::class)], version = 1,
        exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class LocalMangaDatabase : RoomDatabase() {

    abstract fun dao(): LocalMangaDao
}
