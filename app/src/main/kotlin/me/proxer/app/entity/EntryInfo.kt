package me.proxer.app.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * @author Ruben Gees
 */
data class EntryInfo(val name: String?, val totalEpisodes: Int?) : Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField val CREATOR: Parcelable.Creator<EntryInfo> = object : Parcelable.Creator<EntryInfo> {
            override fun createFromParcel(source: Parcel): EntryInfo = EntryInfo(source)
            override fun newArray(size: Int): Array<EntryInfo?> = arrayOfNulls(size)
        }
    }

    private constructor(source: Parcel) : this(source.readString(),
            source.readValue(EntryInfo::class.java.classLoader) as Int?)

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeValue(totalEpisodes)
    }

    fun isComplete() = name != null && totalEpisodes != null
}