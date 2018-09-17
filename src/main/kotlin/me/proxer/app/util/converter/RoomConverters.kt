package me.proxer.app.util.converter

import androidx.room.TypeConverter
import me.proxer.library.enums.Category
import me.proxer.library.enums.Device
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Genre
import me.proxer.library.enums.Language
import me.proxer.library.enums.License
import me.proxer.library.enums.MediaState
import me.proxer.library.enums.Medium
import me.proxer.library.enums.MessageAction
import me.proxer.library.enums.TagSubType
import me.proxer.library.enums.TagType
import me.proxer.library.util.ProxerUtils
import java.util.Date

/**
 * @author Ruben Gees
 */
@Suppress("unused")
class RoomConverters {

    private companion object {
        private const val DELIMITER = ";"
    }

    @TypeConverter
    fun fromGenres(value: MutableSet<Genre>?) = value
        ?.asSequence()
        ?.map { ProxerUtils.getSafeApiEnumName(it) }
        ?.joinToString(DELIMITER)

    @TypeConverter
    fun toGenres(value: String?) = value?.split(DELIMITER)
        ?.asSequence()
        ?.filter { it.isNotBlank() }
        ?.map { ProxerUtils.toSafeApiEnum(Genre::class.java, it) }
        ?.toSet()

    @TypeConverter
    fun fromFskConstraints(value: MutableSet<FskConstraint>?) = value
        ?.asSequence()
        ?.map { ProxerUtils.getSafeApiEnumName(it) }
        ?.joinToString(DELIMITER)

    @TypeConverter
    fun toFskConstraints(value: String?) = value?.split(DELIMITER)
        ?.asSequence()
        ?.filter { it.isNotBlank() }
        ?.map { ProxerUtils.toSafeApiEnum(FskConstraint::class.java, it) }
        ?.toSet()

    @TypeConverter
    fun fromTimestamp(value: Long?) = value?.let { Date(it) }

    @TypeConverter
    fun toTimestamp(date: Date?) = date?.time

    @TypeConverter
    fun fromLanguage(value: Language?) = value?.let { ProxerUtils.getSafeApiEnumName(it) }

    @TypeConverter
    fun toLanguage(value: String?) = value?.let { ProxerUtils.toSafeApiEnum(Language::class.java, it) }

    @TypeConverter
    fun fromMedium(value: Medium?) = value?.let { ProxerUtils.getSafeApiEnumName(it) }

    @TypeConverter
    fun toMedium(value: String?) = value?.let { ProxerUtils.toSafeApiEnum(Medium::class.java, it) }

    @TypeConverter
    fun fromMediaState(value: MediaState?) = value?.let { ProxerUtils.getSafeApiEnumName(it) }

    @TypeConverter
    fun toMediaState(value: String?) = value?.let { ProxerUtils.toSafeApiEnum(MediaState::class.java, it) }

    @TypeConverter
    fun fromCategory(value: Category?) = value?.let { ProxerUtils.getSafeApiEnumName(it) }

    @TypeConverter
    fun toCategory(value: String?) = value?.let { ProxerUtils.toSafeApiEnum(Category::class.java, it) }

    @TypeConverter
    fun fromLicense(value: License?) = value?.let { ProxerUtils.getSafeApiEnumName(it) }

    @TypeConverter
    fun toLicense(value: String?) = value?.let { ProxerUtils.toSafeApiEnum(License::class.java, it) }

    @TypeConverter
    fun fromMessageAction(value: MessageAction?) = value?.let { ProxerUtils.getSafeApiEnumName(it) }

    @TypeConverter
    fun toMessageAction(value: String?) = value?.let { ProxerUtils.toSafeApiEnum(MessageAction::class.java, it) }

    @TypeConverter
    fun fromDevice(value: Device?) = value?.let { ProxerUtils.getSafeApiEnumName(it) }

    @TypeConverter
    fun toDevice(value: String?) = value?.let { ProxerUtils.toSafeApiEnum(Device::class.java, it) }

    @TypeConverter
    fun fromTagType(value: TagType?) = value?.let { ProxerUtils.getSafeApiEnumName(it) }

    @TypeConverter
    fun toTagType(value: String?) = value?.let { ProxerUtils.toSafeApiEnum(TagType::class.java, it) }

    @TypeConverter
    fun fromTagSubType(value: TagSubType?) = value?.let { ProxerUtils.getSafeApiEnumName(it) }

    @TypeConverter
    fun toTagSubType(value: String?) = value?.let { ProxerUtils.toSafeApiEnum(TagSubType::class.java, it) }
}
