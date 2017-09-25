package me.proxer.app.manga.local

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import me.proxer.app.util.converter.RoomConverters
import me.proxer.app.util.converter.RoomJavaConverters

/**
 * @author Ruben Gees
 */
@Database(entities = arrayOf(LocalEntryCore::class, LocalMangaChapter::class, LocalMangaPage::class), version = 1,
        exportSchema = false)
@TypeConverters(RoomConverters::class, RoomJavaConverters::class)
abstract class LocalMangaDatabase : RoomDatabase() {

    abstract fun dao(): LocalMangaDao
}
