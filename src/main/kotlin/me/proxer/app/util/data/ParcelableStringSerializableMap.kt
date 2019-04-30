package me.proxer.app.util.data

import android.os.Parcel
import android.os.Parcelable
import me.proxer.app.util.extension.readSerializableSafely
import me.proxer.app.util.extension.readStringSafely
import java.io.Serializable

/**
 * Parcelable Map for String to Serializable (e.g. Enums) pairs. This cannot actually implement the Map interface,
 * because of the terrible way Parcelable is implemented in Android.
 *
 * @author Ruben Gees
 */
class ParcelableStringSerializableMap<T : Serializable> : Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<ParcelableStringSerializableMap<*>> {
            override fun createFromParcel(source: Parcel) = ParcelableStringSerializableMap<Serializable>(source)
            override fun newArray(size: Int) = arrayOfNulls<ParcelableStringSerializableMap<Serializable>?>(size)
        }
    }

    val size get() = internalMap.size
    val entries get() = internalMap.entries

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

    operator fun set(key: String, value: T) {
        internalMap[key] = value
    }

    operator fun get(key: String) = internalMap[key]
}
