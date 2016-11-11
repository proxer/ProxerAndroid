package com.proxerme.app.entitiy

import android.os.Parcel
import android.os.Parcelable
import com.proxerme.library.connection.messenger.entity.Message

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class LocalMessage : Message, Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField val CREATOR: Parcelable.Creator<LocalMessage> = object : Parcelable.Creator<LocalMessage> {
            override fun createFromParcel(source: Parcel): LocalMessage = LocalMessage(source)
            override fun newArray(size: Int): Array<LocalMessage?> = arrayOfNulls(size)
        }
    }

    val localId: Long

    constructor(localId: Long, id: String, conferenceId: String, userId: String,
                username: String, message: String, action: String, time: Long,
                device: String) : super(id, conferenceId, userId, username, message, action, time, device) {
        this.localId = localId
    }

    private constructor(source: Parcel) : super(source) {
        this.localId = source.readLong()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        if (!super.equals(other)) return false

        other as LocalMessage

        if (localId != other.localId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + localId.hashCode()
        return result
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)

        dest.writeLong(localId)
    }
}