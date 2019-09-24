package me.proxer.app.media

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.proxer.app.util.converter.RoomConverters

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
@Database(entities = [LocalTag::class], version = 2)
@TypeConverters(RoomConverters::class)
abstract class TagDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Nothing to do here, needed due to a bug.
            }
        }
    }

    abstract fun dao(): TagDao
}
