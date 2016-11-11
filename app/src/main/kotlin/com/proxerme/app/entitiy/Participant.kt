package com.proxerme.app.entitiy

import android.os.Parcel
import android.os.Parcelable

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
data class Participant(val username: String, val imageId: String) : Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField val CREATOR: Parcelable.Creator<Participant> = object : Parcelable.Creator<Participant> {
            override fun createFromParcel(source: Parcel): Participant = Participant(source)
            override fun newArray(size: Int): Array<Participant?> = arrayOfNulls(size)
        }
    }

    private constructor(source: Parcel) : this(source.readString(), source.readString())

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(username)
        dest.writeString(imageId)
    }
}