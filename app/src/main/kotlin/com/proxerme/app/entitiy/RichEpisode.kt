package com.proxerme.app.entitiy

import android.os.Parcel
import android.os.Parcelable
import com.proxerme.library.connection.info.entity.Episode
import com.proxerme.library.connection.info.entity.Hoster
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class RichEpisode : Parcelable {

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<RichEpisode> = object : Parcelable.Creator<RichEpisode> {
            override fun createFromParcel(source: Parcel): RichEpisode = RichEpisode(source)
            override fun newArray(size: Int): Array<RichEpisode?> = arrayOfNulls(size)
        }
    }

    val userState: Int
    val number: Int
    val languageHosterMap: Map<String, Array<Hoster>?>

    constructor(userState: Int, episodes: List<Episode>) {
        if (episodes.isEmpty()) {
            throw IllegalArgumentException("At least one episode has to be passed.")
        }

        this.userState = userState
        this.number = episodes.first().number
        this.languageHosterMap = episodes.associate { it.language to it.hosters }
    }

    constructor(source: Parcel) {
        userState = source.readInt()
        number = source.readInt()

        val parcelledMap = HashMap<String, Array<Hoster>?>()

        for (i in 0 until source.readInt()) {
            parcelledMap.put(source.readString(), source.createTypedArray(Hoster.CREATOR))
        }

        languageHosterMap = parcelledMap
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(userState)
        dest.writeInt(number)
        dest.writeInt(languageHosterMap.size)

        languageHosterMap.forEach {
            dest.writeString(it.key)
            dest.writeTypedArray(it.value, 0)
        }
    }
}