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
        ?.map { ProxerUtils.getApiEnumName(it) }
        ?.joinToString(DELIMITER)

    @TypeConverter
    fun toGenres(value: String?) = value?.split(DELIMITER)
        ?.asSequence()
        ?.filter { it.isNotBlank() }
        ?.map {
            ProxerUtils.toApiEnum(Genre::class.java, it)
                ?: throw IllegalArgumentException("enum is null: $it")
        }
        ?.toSet()

    @TypeConverter
    fun fromFskConstraints(value: MutableSet<FskConstraint>?) = value
        ?.asSequence()
        ?.map { ProxerUtils.getApiEnumName(it) }
        ?.joinToString(DELIMITER)

    @TypeConverter
    fun toFskConstraints(value: String?) = value?.split(DELIMITER)
        ?.asSequence()
        ?.filter { it.isNotBlank() }
        ?.map {
            ProxerUtils.toApiEnum(FskConstraint::class.java, it)
                ?: throw IllegalArgumentException("enum is null: $it")
        }
        ?.toSet()

    @TypeConverter
    fun fromTimestamp(value: Long?) = value?.let { Date(it) }

    @TypeConverter
    fun toTimestamp(date: Date?) = date?.time

    @TypeConverter
    fun fromLanguage(value: Language?) = value?.let { ProxerUtils.getApiEnumName(it) }

    @TypeConverter
    fun toLanguage(value: String?) = value?.let { ProxerUtils.toApiEnum(Language::class.java, it) }

    @TypeConverter
    fun fromMedium(value: Medium?) = value?.let { ProxerUtils.getApiEnumName(it) }

    @TypeConverter
    fun toMedium(value: String?) = value?.let { ProxerUtils.toApiEnum(Medium::class.java, it) }

    @TypeConverter
    fun fromMediaState(value: MediaState?) = value?.let { ProxerUtils.getApiEnumName(it) }

    @TypeConverter
    fun toMediaState(value: String?) = value?.let { ProxerUtils.toApiEnum(MediaState::class.java, it) }

    @TypeConverter
    fun fromCategory(value: Category?) = value?.let { ProxerUtils.getApiEnumName(it) }

    @TypeConverter
    fun toCategory(value: String?) = value?.let { ProxerUtils.toApiEnum(Category::class.java, it) }

    @TypeConverter
    fun fromLicense(value: License?) = value?.let { ProxerUtils.getApiEnumName(it) }

    @TypeConverter
    fun toLicense(value: String?) = value?.let { ProxerUtils.toApiEnum(License::class.java, it) }

    @TypeConverter
    fun fromMessageAction(value: MessageAction?) = value?.let { ProxerUtils.getApiEnumName(it) }

    @TypeConverter
    fun toMessageAction(value: String?) = value?.let { ProxerUtils.toApiEnum(MessageAction::class.java, it) }

    @TypeConverter
    fun fromDevice(value: Device?) = value?.let { ProxerUtils.getApiEnumName(it) }

    @TypeConverter
    fun toDevice(value: String?) = value?.let { ProxerUtils.toApiEnum(Device::class.java, it) }

    @TypeConverter
    fun fromTagType(value: TagType?) = value?.let { ProxerUtils.getApiEnumName(it) }

    @TypeConverter
    fun toTagType(value: String?) = value?.let { ProxerUtils.toApiEnum(TagType::class.java, it) }

    @TypeConverter
    fun fromTagSubType(value: TagSubType?) = value?.let { ProxerUtils.getApiEnumName(it) }

    @TypeConverter
    fun toTagSubType(value: String?) = value?.let { ProxerUtils.toApiEnum(TagSubType::class.java, it) }
}
