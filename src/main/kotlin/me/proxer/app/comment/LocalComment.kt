package me.proxer.app.comment

import android.os.Parcel
import android.os.Parcelable
import me.proxer.app.ui.view.bbcode.toSimpleBBTree
import me.proxer.app.util.extension.readStringSafely
import me.proxer.library.entity.info.RatingDetails
import me.proxer.library.enums.UserMediaProgress
import me.proxer.library.util.ProxerUtils

/**
 * @author Ruben Gees
 */
data class LocalComment(
    val id: String,
    val entryId: String,
    val mediaProgress: UserMediaProgress,
    val ratingDetails: RatingDetails,
    val content: String,
    val overallRating: Int,
    val episode: Int
) : Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<LocalComment> {
            override fun createFromParcel(parcel: Parcel) = LocalComment(parcel)
            override fun newArray(size: Int): Array<LocalComment?> = arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readStringSafely(),
        parcel.readStringSafely(),
        ProxerUtils.toSafeApiEnum(parcel.readStringSafely()),
        RatingDetails(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt()),
        parcel.readStringSafely(),
        parcel.readInt(),
        parcel.readInt()
    )

    val parsedContent = content.toSimpleBBTree()

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(entryId)
        parcel.writeString(ProxerUtils.getSafeApiEnumName(mediaProgress))
        parcel.writeInt(ratingDetails.genre)
        parcel.writeInt(ratingDetails.story)
        parcel.writeInt(ratingDetails.animation)
        parcel.writeInt(ratingDetails.characters)
        parcel.writeInt(ratingDetails.music)
        parcel.writeString(content)
        parcel.writeInt(overallRating)
        parcel.writeInt(episode)
    }

    override fun describeContents() = 0
}
