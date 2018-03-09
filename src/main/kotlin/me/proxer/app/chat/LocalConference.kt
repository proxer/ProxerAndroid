package me.proxer.app.chat

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import me.proxer.library.entity.messenger.Conference
import java.util.Date

/**
 * @author Ruben Gees
 */
@Entity(tableName = "conferences")
data class LocalConference(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val topic: String,
    val customTopic: String,
    val participantAmount: Int,
    val image: String,
    val imageType: String,
    val isGroup: Boolean,
    val localIsRead: Boolean,
    val isRead: Boolean,
    val date: Date,
    val unreadMessageAmount: Int,
    val lastReadMessageId: String,
    val isFullyLoaded: Boolean
) : Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<LocalConference> {
            override fun createFromParcel(parcel: Parcel) = LocalConference(parcel)
            override fun newArray(size: Int): Array<LocalConference?> = arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        Date(parcel.readLong()),
        parcel.readInt(),
        parcel.readString(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(topic)
        parcel.writeString(customTopic)
        parcel.writeInt(participantAmount)
        parcel.writeString(image)
        parcel.writeString(imageType)
        parcel.writeByte(if (isGroup) 1 else 0)
        parcel.writeByte(if (localIsRead) 1 else 0)
        parcel.writeByte(if (isRead) 1 else 0)
        parcel.writeLong(date.time)
        parcel.writeInt(unreadMessageAmount)
        parcel.writeString(lastReadMessageId)
        parcel.writeByte(if (isFullyLoaded) 1 else 0)
    }

    override fun describeContents() = 0

    fun toNonLocalConference() = Conference(id.toString(), topic, customTopic, participantAmount, image, imageType,
        isGroup, isRead, date, unreadMessageAmount, lastReadMessageId)
}
