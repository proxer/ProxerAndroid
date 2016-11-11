package com.proxerme.app.util

import android.os.Parcel
import android.os.Parcelable
import android.support.v4.util.LongSparseArray

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ParcelableLongSparseArray : LongSparseArray<Boolean>, Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField val CREATOR: Parcelable.Creator<ParcelableLongSparseArray> = object : Parcelable.Creator<ParcelableLongSparseArray> {
            override fun createFromParcel(source: Parcel): ParcelableLongSparseArray = ParcelableLongSparseArray(source)
            override fun newArray(size: Int): Array<ParcelableLongSparseArray?> = arrayOfNulls(size)
        }
    }

    constructor() : super()

    private constructor(source: Parcel) {
        val size = source.readInt()
        val keyArray = LongArray(size)
        val valueArray = BooleanArray(size)

        source.readLongArray(keyArray)
        source.readBooleanArray(valueArray)

        for (i in 0..size - 1) {
            put(keyArray[i], valueArray[i])
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val size = size()
        val keyArray = LongArray(size())
        val valueArray = BooleanArray(size())

        for (i in 0..size - 1) {
            keyArray[i] = keyAt(i)
            valueArray[i] = valueAt(i)
        }

        dest.writeInt(size)
        dest.writeLongArray(keyArray)
        dest.writeBooleanArray(valueArray)
    }
}