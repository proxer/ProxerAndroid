package me.proxer.app.manga.local

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import me.proxer.library.entity.info.AdaptionInfo
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.Category
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Genre
import me.proxer.library.enums.License
import me.proxer.library.enums.MediaState
import me.proxer.library.enums.Medium

/**
 * @author Ruben Gees
 */
@Entity(tableName = "entries")
data class LocalEntryCore(@PrimaryKey(autoGenerate = true) val id: Long, val name: String,
                          val genres: MutableSet<Genre>, val fskConstraints: MutableSet<FskConstraint>,
                          val description: String, val medium: Medium, val episodeAmount: Int, val state: MediaState,
                          val ratingSum: Int, val ratingAmount: Int, val clicks: Int, val category: Category,
                          val license: License, @Embedded(prefix = "adaption_") val adaptionInfo: AdaptionInfo) {

    fun toNonLocalEntryCore() = EntryCore(id.toString(), name, genres, fskConstraints, description, medium,
            episodeAmount, state, ratingSum, ratingAmount, clicks, category, license, adaptionInfo)
}
