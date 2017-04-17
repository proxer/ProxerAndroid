package me.proxer.app.util

import android.os.Parcel
import android.os.Parcelable

/**
 * @author Ruben Gees
 */
class ParcelableStringBooleanMap : Parcelable {

    companion object {
        private const val ZERO_BYTE: Byte = 0

        @JvmStatic val CREATOR = object : Parcelable.Creator<ParcelableStringBooleanMap> {
            override fun createFromParcel(source: Parcel): ParcelableStringBooleanMap {
                return ParcelableStringBooleanMap(source)
            }

            override fun newArray(size: Int): Array<ParcelableStringBooleanMap?> {
                return arrayOfNulls(size)
            }
        }
    }

    private val internalMap = LinkedHashMap<String, Boolean>()

    constructor() : super()
    internal constructor(source: Parcel) {
        for (i in 0 until source.readInt()) {
            put(source.readString(), source.readByte() != ZERO_BYTE)
        }
    }

    override fun describeContents() = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(internalMap.size)

        internalMap.entries.forEach {
            dest.writeString(it.key)
            dest.writeByte(if (it.value) 1 else 0)
        }
    }

    fun put(key: String, value: Boolean) = internalMap.put(key, value)
    fun remove(key: String) = internalMap.remove(key)
    operator fun get(key: String) = internalMap.get(key)
}
