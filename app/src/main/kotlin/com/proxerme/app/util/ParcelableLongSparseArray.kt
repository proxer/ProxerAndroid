package com.proxerme.app.util

import android.os.Parcel
import android.os.Parcelable
import android.support.v4.util.LongSparseArray

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ParcelableLongBooleanSparseArray : LongSparseArray<Boolean>, Parcelable {

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<ParcelableLongBooleanSparseArray> = object : Parcelable.Creator<ParcelableLongBooleanSparseArray> {
            override fun createFromParcel(source: Parcel): ParcelableLongBooleanSparseArray = ParcelableLongBooleanSparseArray(source)
            override fun newArray(size: Int): Array<ParcelableLongBooleanSparseArray?> = arrayOfNulls(size)
        }
    }

    constructor() : super()
    constructor(initialCapacity: Int) : super(initialCapacity)

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