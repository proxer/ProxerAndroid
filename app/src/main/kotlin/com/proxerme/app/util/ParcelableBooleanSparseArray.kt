package com.proxerme.app.util

import android.os.Parcel
import android.os.Parcelable
import android.util.SparseBooleanArray

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ParcelableBooleanSparseArray : SparseBooleanArray, Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField val CREATOR: Parcelable.Creator<ParcelableBooleanSparseArray> = object : Parcelable.Creator<ParcelableBooleanSparseArray> {
            override fun createFromParcel(source: Parcel): ParcelableBooleanSparseArray = ParcelableBooleanSparseArray(source)
            override fun newArray(size: Int): Array<ParcelableBooleanSparseArray?> = arrayOfNulls(size)
        }
    }

    constructor() : super()

    private constructor(source: Parcel) {
        val size = source.readInt()
        val keyArray = IntArray(size)
        val valueArray = BooleanArray(size)

        source.readIntArray(keyArray)
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
        val keyArray = IntArray(size())
        val valueArray = BooleanArray(size())

        for (i in 0..size - 1) {
            keyArray[i] = keyAt(i)
            valueArray[i] = valueAt(i)
        }

        dest.writeInt(size)
        dest.writeIntArray(keyArray)
        dest.writeBooleanArray(valueArray)
    }
}