package me.proxer.app.util.data

import android.os.Parcel
import android.os.Parcelable
import me.proxer.app.util.extension.readSerializableSafely
import me.proxer.app.util.extension.readStringSafely
import java.io.Serializable

/**
 * @author Ruben Gees
 */
class ParcelableStringSerializableMap<T : Serializable> : MutableMap<String, T>, Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<ParcelableStringSerializableMap<*>> {
            override fun createFromParcel(source: Parcel) = ParcelableStringSerializableMap<Serializable>(source)
            override fun newArray(size: Int) = arrayOfNulls<ParcelableStringSerializableMap<Serializable>?>(size)
        }
    }

    override val size get() = internalMap.size
    override val entries get() = internalMap.entries
    override val keys get() = internalMap.keys
    override val values get() = internalMap.values

    private val internalMap = LinkedHashMap<String, T>()

    constructor()

    @Suppress("UNCHECKED_CAST")
    internal constructor(source: Parcel) {
        repeat(source.readInt()) {
            this[source.readStringSafely()] = source.readSerializableSafely() as T
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(size)

        entries.forEach { (key, value) ->
            dest.writeString(key)
            dest.writeSerializable(value)
        }
    }

    override fun describeContents() = 0

    override fun containsKey(key: String) = internalMap.containsKey(key)
    override fun containsValue(value: T) = internalMap.containsValue(value)
    override fun get(key: String) = internalMap[key]
    override fun isEmpty() = internalMap.isEmpty()
    override fun clear() = internalMap.clear()
    override fun put(key: String, value: T) = internalMap.put(key, value)
    override fun putAll(from: Map<out String, T>) = internalMap.putAll(from)
    override fun remove(key: String) = internalMap.remove(key)
}
