package me.proxer.app.util.data

import android.os.Parcel
import android.os.Parcelable

/**
 * @author Ruben Gees
 */
class ParcelableStringBooleanMap : Parcelable {

    companion object {
        private const val ZERO_BYTE: Byte = 0

        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<ParcelableStringBooleanMap> {
            override fun createFromParcel(source: Parcel) = ParcelableStringBooleanMap(source)
            override fun newArray(size: Int) = arrayOfNulls<ParcelableStringBooleanMap?>(size)
        }
    }

    val size: Int
        get() = internalMap.size

    private val internalMap = LinkedHashMap<String, Boolean>()

    constructor() : super()

    internal constructor(source: Parcel) {
        (0 until source.readInt()).forEach {
            put(source.readString(), source.readByte() != ParcelableStringBooleanMap.Companion.ZERO_BYTE)
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(internalMap.size)

        internalMap.entries.forEach {
            dest.writeString(it.key)
            dest.writeByte(if (it.value) 1 else 0)
        }
    }

    override fun describeContents() = 0

    operator fun get(key: String) = internalMap[key]
    fun put(key: String, value: Boolean) = internalMap.put(key, value)
    fun remove(key: String) = internalMap.remove(key)
    fun clear() = internalMap.clear()
}
