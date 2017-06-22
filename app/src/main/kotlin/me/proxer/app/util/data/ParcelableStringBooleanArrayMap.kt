package me.proxer.app.util.data

import android.os.Parcel
import android.os.Parcelable
import android.util.SparseBooleanArray
import java.util.*

/**
 * @author Ruben Gees
 */
class ParcelableStringBooleanArrayMap : Parcelable {

    companion object {

        @Suppress("unused")
        @JvmStatic val CREATOR = object : Parcelable.Creator<ParcelableStringBooleanArrayMap> {
            override fun createFromParcel(source: Parcel): ParcelableStringBooleanArrayMap {
                return ParcelableStringBooleanArrayMap(source)
            }

            override fun newArray(size: Int): Array<ParcelableStringBooleanArrayMap?> {
                return arrayOfNulls(size)
            }
        }
    }

    private val internalMap = LinkedHashMap<String, SparseBooleanArray>()

    constructor() : super()
    internal constructor(source: Parcel) {
        (0 until source.readInt()).forEach {
            internalMap.put(source.readString(), source.readSparseBooleanArray())
        }
    }

    override fun describeContents() = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(internalMap.size)

        internalMap.entries.forEach {
            dest.writeString(it.key)
            dest.writeSparseBooleanArray(it.value)
        }
    }

    fun put(key: String, value: SparseBooleanArray) = internalMap.put(key, value)
    operator fun get(key: String) = internalMap[key]
}