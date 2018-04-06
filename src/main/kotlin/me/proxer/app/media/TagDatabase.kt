package me.proxer.app.media

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
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
