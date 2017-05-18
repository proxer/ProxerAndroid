package me.proxer.app.entity.chat

import android.os.Parcel
import android.os.Parcelable
import me.proxer.library.entitiy.messenger.Conference
import java.util.*

/**
 * @author Ruben Gees
 */
data class LocalConference(val localId: Long, val id: String, val topic: String, val customTopic: String,
                           val participantAmount: Int, val image: String, val imageType: String, val isGroup: Boolean,
                           val localIsRead: Boolean, val isRead: Boolean, val date: Date, val unreadMessageAmount: Int,
                           val lastReadMessageId: String) : Parcelable {

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<LocalConference> = object : Parcelable.Creator<LocalConference> {
            override fun createFromParcel(source: Parcel): LocalConference = LocalConference(source)
            override fun newArray(size: Int): Array<LocalConference?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(
            source.readLong(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readInt(),
            source.readString(),
            source.readString(),
            1 == source.readInt(),
            1 == source.readInt(),
            1 == source.readInt(),
            source.readSerializable() as Date,
            source.readInt(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(localId)
        dest.writeString(id)
        dest.writeString(topic)
        dest.writeString(customTopic)
        dest.writeInt(participantAmount)
        dest.writeString(image)
        dest.writeString(imageType)
        dest.writeInt((if (isGroup) 1 else 0))
        dest.writeInt((if (localIsRead) 1 else 0))
        dest.writeInt((if (isRead) 1 else 0))
        dest.writeSerializable(date)
        dest.writeInt(unreadMessageAmount)
        dest.writeString(lastReadMessageId)
    }

    fun toNonLocalConference(): Conference {
        return Conference(id, topic, customTopic, participantAmount, image, imageType, isGroup, isRead, date,
                unreadMessageAmount, lastReadMessageId)
    }
}

fun Conference.toLocalConference(localId: Long): LocalConference {
    return LocalConference(localId, id, topic, customTopic, participantAmount, image, imageType, isGroup,
            isRead, isRead, date, unreadMessageAmount, lastReadMessageId)
}