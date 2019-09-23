package me.proxer.app.chat.prv.sync

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

    abstract fun dao(): MessengerDao
}
