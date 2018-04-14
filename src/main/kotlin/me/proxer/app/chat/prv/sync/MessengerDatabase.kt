package me.proxer.app.chat.prv.sync

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.util.converter.RoomConverters

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
@Database(entities = [(LocalConference::class), (LocalMessage::class)], version = 1, exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class MessengerDatabase : RoomDatabase() {

    abstract fun dao(): MessengerDao
}
