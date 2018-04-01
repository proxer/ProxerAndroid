package me.proxer.app.media.list

import android.os.Parcel
import android.os.Parcelable
import me.proxer.library.entity.ProxerIdItem

/**
 * @author Ruben Gees
 */
data class ParcelableTag(
    private val id: String,
    val name: String
) : ProxerIdItem, Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<ParcelableTag> {
            override fun createFromParcel(parcel: Parcel) = ParcelableTag(parcel)
            override fun newArray(size: Int): Array<ParcelableTag?> = arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString()
    )

    override fun getId() = id

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
    }

    override fun describeContents() = 0
}
