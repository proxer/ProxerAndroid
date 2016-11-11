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
        @Suppress("unused")
        @JvmField val CREATOR: Parcelable.Creator<RichEpisode> = object : Parcelable.Creator<RichEpisode> {
            override fun createFromParcel(source: Parcel): RichEpisode = RichEpisode(source)
            override fun newArray(size: Int): Array<RichEpisode?> = arrayOfNulls(size)
        }
    }

    val userState: Int
    val number: Int
    val totalEpisodes: Int
    val title: String?
    val languageHosterMap: Map<String, Array<Hoster>?>

    constructor(userState: Int, totalEpisodes: Int, episodes: List<Episode>) {
        if (episodes.isEmpty()) {
            throw IllegalArgumentException("At least one episode has to be passed.")
        }

        this.userState = userState
        this.totalEpisodes = totalEpisodes
        this.number = episodes.first().number
        this.title = episodes.first().title
        this.languageHosterMap = episodes.associate { it.language to it.hosters }
    }

    constructor(source: Parcel) {
        userState = source.readInt()
        totalEpisodes = source.readInt()
        number = source.readInt()
        title = source.readString()

        val parcelledMap = HashMap<String, Array<Hoster>?>()

        for (i in 0 until source.readInt()) {
            parcelledMap.put(source.readString(), source.createTypedArray(Hoster.CREATOR))
        }

        languageHosterMap = parcelledMap
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(userState)
        dest.writeInt(totalEpisodes)
        dest.writeInt(number)
        dest.writeString(title)
        dest.writeInt(languageHosterMap.size)

        languageHosterMap.forEach {
            dest.writeString(it.key)
            dest.writeTypedArray(it.value, 0)
        }
    }

    fun isAnime(): Boolean {
        return title == null
    }
}