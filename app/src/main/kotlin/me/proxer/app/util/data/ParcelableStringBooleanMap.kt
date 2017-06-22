package me.proxer.app.util.data

/**
 * @author Ruben Gees
 */
class ParcelableStringBooleanMap : android.os.Parcelable {

    companion object {
        private const val ZERO_BYTE: Byte = 0

        @Suppress("unused")
        @JvmStatic val CREATOR = object : android.os.Parcelable.Creator<ParcelableStringBooleanMap> {
            override fun createFromParcel(source: android.os.Parcel): me.proxer.app.util.data.ParcelableStringBooleanMap {
                return me.proxer.app.util.data.ParcelableStringBooleanMap(source)
            }

            override fun newArray(size: Int): Array<me.proxer.app.util.data.ParcelableStringBooleanMap?> {
                return arrayOfNulls(size)
            }
        }
    }

    val size: Int
        get() = internalMap.size

    private val internalMap = LinkedHashMap<String, Boolean>()

    constructor() : super()
    internal constructor(source: android.os.Parcel) {
        (0 until source.readInt()).forEach {
            put(source.readString(), source.readByte() != me.proxer.app.util.data.ParcelableStringBooleanMap.Companion.ZERO_BYTE)
        }
    }

    override fun describeContents() = 0
    override fun writeToParcel(dest: android.os.Parcel, flags: Int) {
        dest.writeInt(internalMap.size)

        internalMap.entries.forEach {
            dest.writeString(it.key)
            dest.writeByte(if (it.value) 1 else 0)
        }
    }

    operator fun get(key: String) = internalMap[key]

    fun put(key: String, value: Boolean) = internalMap.put(key, value)
    fun remove(key: String) = internalMap.remove(key)
    fun clear() = internalMap.clear()
}
