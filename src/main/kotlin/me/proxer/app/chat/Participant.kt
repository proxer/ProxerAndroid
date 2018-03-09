package me.proxer.app.chat

import android.os.Parcel
import android.os.Parcelable

/**
 * @author Ruben Gees
 */
data class Participant(val username: String, val image: String = "") : Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<Participant> {
            override fun createFromParcel(parcel: Parcel) = Participant(parcel)
            override fun newArray(size: Int): Array<Participant?> = arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(username)
        parcel.writeString(image)
    }

    override fun describeContents() = 0
}
