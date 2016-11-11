package com.proxerme.app.entitiy

import android.os.Parcel
import android.os.Parcelable
import com.proxerme.library.connection.messenger.entity.Conference

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class LocalConference : Conference, Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField val CREATOR: Parcelable.Creator<LocalConference> = object : Parcelable.Creator<LocalConference> {
            override fun createFromParcel(source: Parcel): LocalConference = LocalConference(source)
            override fun newArray(size: Int): Array<LocalConference?> = arrayOfNulls(size)
        }
    }

    val localId: Long
    var isReadLocal: Boolean

    constructor(localId: Long, id: String, topic: String, customTopic: String,
                participantAmount: Int, imageType: String?, imageId: String?, isGroup: Boolean,
                localIsRead: Boolean, isRead: Boolean, time: Long, unreadMessageAmount: Int,
                lastReadMessageId: String) : super(id, topic, customTopic, participantAmount,
            imageType, imageId, isGroup, isRead, time, unreadMessageAmount, lastReadMessageId) {
        this.localId = localId
        this.isReadLocal = localIsRead
    }

    private constructor(source: Parcel) : super(source) {
        this.localId = source.readLong()
        this.isReadLocal = source.readInt() == 1
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)

        dest.writeLong(localId)
        dest.writeInt(if (isReadLocal) 1 else 0)
    }

    fun toNonLocalConference(): Conference {
        return Conference(id, topic, customTopic, participantAmount, imageType, imageId, isGroup,
                isRead, time, unreadMessageAmount, lastReadMessageId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        if (!super.equals(other)) return false

        other as LocalConference

        if (localId != other.localId) return false
        if (isReadLocal != other.isReadLocal) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + localId.hashCode()
        result = 31 * result + isReadLocal.hashCode()
        return result
    }
}