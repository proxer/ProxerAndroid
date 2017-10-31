package me.proxer.app.util.data

import android.os.Parcel
import android.os.Parcelable

/**
 * @author Ruben Gees
 */
class ParcelableStringBooleanMap : Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<ParcelableStringBooleanMap> {
            override fun createFromParcel(source: Parcel) = ParcelableStringBooleanMap(source)
            override fun newArray(size: Int) = arrayOfNulls<ParcelableStringBooleanMap?>(size)
        }
    }

    val size get() = internalMap.size
    val keys get() = internalMap.keys
    val entries get() = internalMap.entries

    private val internalMap = LinkedHashMap<String, Boolean>()

    constructor()

    constructor(items: Iterable<String>) {
        items.forEach {
            put(it, true)
        }
    }

    internal constructor(source: Parcel) {
        (0 until source.readInt()).forEach {
            put(source.readString(), source.readInt() == 1)
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(size)

        entries.forEach {
            dest.writeString(it.key)
            dest.writeInt(if (it.value) 1 else 0)
        }
    }

    override fun describeContents() = 0

    operator fun get(key: String) = internalMap[key]

    fun containsKey(key: String) = internalMap.containsKey(key)
    fun put(key: String, value: Boolean) = internalMap.put(key, value)
    fun remove(key: String) = internalMap.remove(key)
    fun clear() = internalMap.clear()

    fun putOrRemove(key: String) {
        when (containsKey(key)) {
            true -> remove(key)
            false -> put(key, true)
        }
    }
}
