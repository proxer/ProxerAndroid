package me.proxer.app.chat.prv

import android.os.Parcel
import android.os.Parcelable
import me.proxer.app.util.extension.readStringSafely

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
        parcel.readStringSafely(),
        parcel.readStringSafely()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(username)
        parcel.writeString(image)
    }

    override fun describeContents() = 0
}
