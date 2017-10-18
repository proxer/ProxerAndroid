package me.proxer.app.manga.local

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.arch.persistence.room.migration.Migration
import me.proxer.app.util.converter.RoomConverters

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
@Database(entities = [(LocalEntryCore::class), (LocalMangaChapter::class), (LocalMangaPage::class)], version = 2,
        exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class LocalMangaDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_ONE_TWO = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE `entries_tmp` (" +
                        "`id`              INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`name`            TEXT NOT NULL, " +
                        "`genres`          TEXT NOT NULL, " +
                        "`fskConstraints`  TEXT NOT NULL, " +
                        "`description`     TEXT NOT NULL, " +
                        "`medium`          TEXT NOT NULL, " +
                        "`episodeAmount`   INTEGER NOT NULL, " +
                        "`state`           TEXT NOT NULL, " +
                        "`ratingSum`       INTEGER NOT NULL, " +
                        "`ratingAmount`	   INTEGER NOT NULL, " +
                        "`clicks`          INTEGER NOT NULL, " +
                        "`category`	       TEXT NOT NULL, " +
                        "`license`         TEXT NOT NULL, " +
                        "`adaption_id`     TEXT, " +
                        "`adaption_name`   TEXT, " +
                        "`adaption_medium` TEXT)"
                )

                database.execSQL("INSERT INTO `entries_tmp` SELECT * FROM `entries`")
                database.execSQL("DROP TABLE `entries`")
                database.execSQL("ALTER TABLE `entries_tmp` RENAME TO `entries`")
            }
        }
    }

    abstract fun dao(): LocalMangaDao
}
