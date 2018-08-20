package me.proxer.app.media

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.proxer.app.util.converter.RoomConverters

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
@Database(entities = [(LocalTag::class)], version = 1, exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class TagDatabase : RoomDatabase() {

    abstract fun dao(): TagDao
}
