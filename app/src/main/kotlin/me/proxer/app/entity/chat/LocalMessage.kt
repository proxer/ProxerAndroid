package me.proxer.app.entity.chat

import android.os.Parcel
import android.os.Parcelable
import me.proxer.library.entitiy.messenger.Message
import me.proxer.library.enums.Device
import me.proxer.library.enums.MessageAction
import java.util.*

/**
 * @author Ruben Gees
 */
data class LocalMessage(val localId: Long, val id: String, val conferenceId: String, val userId: String,
                        val username: String, val message: String, val action: MessageAction, val date: Date,
                        val device: Device) : Parcelable {

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<LocalMessage> = object : Parcelable.Creator<LocalMessage> {
            override fun createFromParcel(source: Parcel): LocalMessage = LocalMessage(source)
            override fun newArray(size: Int): Array<LocalMessage?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(
            source.readLong(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readSerializable() as MessageAction,
            source.readSerializable() as Date,
            source.readSerializable() as Device
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(localId)
        dest.writeString(id)
        dest.writeString(conferenceId)
        dest.writeString(userId)
        dest.writeString(username)
        dest.writeString(message)
        dest.writeSerializable(action)
        dest.writeSerializable(date)
        dest.writeSerializable(device)
    }

    fun toNonLocalMessage(): Message {
        return Message(id, conferenceId, userId, username, message, action, date, device)
    }
}

fun Message.toLocalMessage(localId: Long): LocalMessage {
    return LocalMessage(localId, id, conferenceId, userId, username, message, action, date, device)
}