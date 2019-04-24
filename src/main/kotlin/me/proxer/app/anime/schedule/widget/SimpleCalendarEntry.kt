package me.proxer.app.anime.schedule.widget

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import me.proxer.app.util.extension.readStringSafely
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class SimpleCalendarEntry(
    val id: String,
    val entryId: String,
    val name: String,
    val episode: Int,
    val date: Instant,
    val uploadDate: Instant
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
        Instant.ofEpochMilli(parcel.readLong()),
        Instant.ofEpochMilli(parcel.readLong())
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(entryId)
        parcel.writeString(name)
        parcel.writeInt(episode)
        parcel.writeLong(date.toEpochMilli())
        parcel.writeLong(uploadDate.toEpochMilli())
    }

    override fun describeContents() = 0
}
