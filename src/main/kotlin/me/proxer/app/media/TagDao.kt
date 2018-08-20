package me.proxer.app.media

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

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
