package me.proxer.app.anime.schedule.widget

import android.os.Parcel
import android.os.Parcelable
import me.proxer.app.util.extension.readStringSafely
import java.util.Date

/**
 * @author Ruben Gees
 */
data class SimpleCalendarEntry(
    val id: String,
    val entryId: String,
    val name: String,
    val episode: Int,
    val date: Date,
    val uploadDate: Date
) : Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<SimpleCalendarEntry> {
            override fun createFromParcel(parcel: Parcel) = SimpleCalendarEntry(parcel)
            override fun newArray(size: Int): Array<SimpleCalendarEntry?> = arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readStringSafely(),
        parcel.readStringSafely(),
        parcel.readStringSafely(),
        parcel.readInt(),
        Date(parcel.readLong()),
        Date(parcel.readLong())
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(entryId)
        parcel.writeString(name)
        parcel.writeInt(episode)
        parcel.writeLong(date.time)
        parcel.writeLong(uploadDate.time)
    }

    override fun describeContents() = 0
}
