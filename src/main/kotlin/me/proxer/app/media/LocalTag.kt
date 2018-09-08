package me.proxer.app.media

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import me.proxer.app.util.extension.readStringSafely
import me.proxer.library.enums.TagSubType
import me.proxer.library.enums.TagType

/**
 * @author Ruben Gees
 */
@Entity(tableName = "tags")
data class LocalTag(
    @PrimaryKey val id: String,
    val type: TagType,
    val name: String,
    val description: String,
    val subType: TagSubType,
    val isSpoiler: Boolean
) : Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<LocalTag> {
            override fun createFromParcel(parcel: Parcel) = LocalTag(parcel)
            override fun newArray(size: Int): Array<LocalTag?> = arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readStringSafely(),
        TagType.values()[parcel.readInt()],
        parcel.readStringSafely(),
        parcel.readStringSafely(),
        TagSubType.values()[parcel.readInt()],
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeInt(type.ordinal)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeInt(subType.ordinal)
        parcel.writeByte(if (isSpoiler) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }
}
