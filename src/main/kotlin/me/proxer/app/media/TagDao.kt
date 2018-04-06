package me.proxer.app.media

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction

/**
 * @author Ruben Gees
 */
@Dao
abstract class TagDao {

    @Transaction
    open fun replaceTags(tags: List<LocalTag>): List<Long> {
        clear()

        return insertTags(tags)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertTags(tags: List<LocalTag>): List<Long>

    @Query("SELECT * FROM tags")
    abstract fun getTags(): List<LocalTag>

    @Query("DELETE FROM tags")
    abstract fun clear()
}
