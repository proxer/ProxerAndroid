package me.proxer.app.util

import android.os.Parcel
import android.os.Parcelable

/**
 * @author Ruben Gees
 */
class ParcelableStringBooleanMap : AbstractMap<String, Boolean>, Parcelable {

    companion object {
        @JvmField val CREATOR = object : Parcelable.Creator<ParcelableStringBooleanMap> {
            override fun createFromParcel(source: Parcel): ParcelableStringBooleanMap {
                return ParcelableStringBooleanMap(source)
            }

            override fun newArray(size: Int): Array<ParcelableStringBooleanMap?> {
                return arrayOfNulls(size)
            }
        }
    }

    private val internalMap = LinkedHashMap<String, Boolean>()

    override val entries: Set<Map.Entry<String, Boolean>>
        get() = internalMap.entries
    override val keys: Set<String>
        get() = internalMap.keys
    override val size: Int
        get() = internalMap.size
    override val values: Collection<Boolean>
        get() = internalMap.values

    constructor()

    internal constructor(source: Parcel) {
        val size = source.readInt()

        for (i in 0 until size) {
            internalMap.put(source.readString(), source.readInt() != 0)
        }
    }

    override fun containsKey(key: String) = internalMap.containsKey(key)
    override fun containsValue(value: Boolean) = internalMap.containsValue(value)
    override fun get(key: String) = internalMap[key]
    override fun isEmpty() = internalMap.isEmpty()

    override fun describeContents() = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(size)

        entries.forEach {
            dest.writeString(it.key)
            dest.writeInt(if (it.value) 1 else 0)
        }
    }

    fun put(key: String, value: Boolean) = internalMap.put(key, true)
    fun remove(key: String) = internalMap.remove(key)
}
