package me.proxer.app.chat.prv.sync

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.util.converter.RoomConverters

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
@Database(entities = [LocalConference::class, LocalMessage::class], version = 1)
@TypeConverters(RoomConverters::class)
abstract class MessengerDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Nothing to do here, needed due to a bug.
            }
        }
    }

    abstract fun dao(): MessengerDao
}
