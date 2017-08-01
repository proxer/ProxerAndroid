package me.proxer.app.util

import android.arch.persistence.room.TypeConverter
import me.proxer.library.enums.*
import me.proxer.library.util.ProxerUtils
import java.util.*

/**
 * @author Ruben Gees
 */
class RoomConverters {

    companion object {
        private const val DELIMITER = ";"

        fun fromGenres(value: Set<Genre>) = value.map { ProxerUtils.getApiEnumName(it) }
                .joinToString(DELIMITER)

        fun toGenres(value: String) = value.split(DELIMITER)
                .filter { it.isNotBlank() }
                .map { ProxerUtils.toApiEnum(Genre::class.java, it) ?: throw IllegalArgumentException() }
                .toSet()

        fun fromFskConstraints(value: Set<FskConstraint>) = value.map { ProxerUtils.getApiEnumName(it) }
                .joinToString(DELIMITER)

        fun toFskConstraints(value: String) = value.split(DELIMITER)
                .filter { it.isNotBlank() }
                .map { ProxerUtils.toApiEnum(FskConstraint::class.java, it) ?: throw IllegalArgumentException() }
                .toSet()
    }

    @TypeConverter
    fun fromTimestamp(value: Long) = Date(value)

    @TypeConverter
    fun toTimestamp(date: Date) = date.time

    @TypeConverter
    fun fromLanguage(value: Language) = ProxerUtils.getApiEnumName(value)

    @TypeConverter
    fun toLanguage(value: String) = ProxerUtils.toApiEnum(Language::class.java, value)

    @TypeConverter
    fun fromMedium(value: Medium) = ProxerUtils.getApiEnumName(value)

    @TypeConverter
    fun toMedium(value: String) = ProxerUtils.toApiEnum(Medium::class.java, value)

    @TypeConverter
    fun fromMediaState(value: MediaState) = ProxerUtils.getApiEnumName(value)

    @TypeConverter
    fun toMediaState(value: String) = ProxerUtils.toApiEnum(MediaState::class.java, value)

    @TypeConverter
    fun fromCategory(value: Category) = ProxerUtils.getApiEnumName(value)

    @TypeConverter
    fun toCategory(value: String) = ProxerUtils.toApiEnum(Category::class.java, value)

    @TypeConverter
    fun fromLicense(value: License) = ProxerUtils.getApiEnumName(value)

    @TypeConverter
    fun toLicense(value: String) = ProxerUtils.toApiEnum(License::class.java, value)
}
