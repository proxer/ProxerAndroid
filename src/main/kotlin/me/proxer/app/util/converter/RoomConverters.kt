package me.proxer.app.util.converter

import androidx.room.TypeConverter
import me.proxer.library.enums.Device
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
    fun fromTimestamp(value: Long?) = value?.let { Date(it) }

    @TypeConverter
    fun toTimestamp(date: Date?) = date?.time

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
