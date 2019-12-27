package me.proxer.app.profile.media

import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.enums.MediaState
import me.proxer.library.enums.Medium
import me.proxer.library.enums.UserMediaProgress

open class LocalUserMediaListEntry(
    override val id: String,
    open val name: String,
    open val episodeAmount: Int,
    open val medium: Medium,
    open val state: MediaState,
    open val commentId: String,
    open val commentContent: String,
    open val mediaProgress: UserMediaProgress,
    open val episode: Int,
    open val rating: Int
) : ProxerIdItem {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalUserMediaListEntry) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (episodeAmount != other.episodeAmount) return false
        if (medium != other.medium) return false
        if (state != other.state) return false
        if (commentId != other.commentId) return false
        if (commentContent != other.commentContent) return false
        if (mediaProgress != other.mediaProgress) return false
        if (episode != other.episode) return false
        if (rating != other.rating) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + episodeAmount
        result = 31 * result + medium.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + commentId.hashCode()
        result = 31 * result + commentContent.hashCode()
        result = 31 * result + mediaProgress.hashCode()
        result = 31 * result + episode
        result = 31 * result + rating
        return result
    }

    override fun toString(): String {
        return "LocalUserMediaListEntry(id='$id', name='$name', episodeAmount=$episodeAmount, medium=$medium, " +
            "state=$state, commentId='$commentId', commentContent='$commentContent', mediaProgress=$mediaProgress, " +
            "episode=$episode, rating=$rating)"
    }

    data class Ucp(
        override val id: String,
        override val name: String,
        override val episodeAmount: Int,
        override val medium: Medium,
        override val state: MediaState,
        override val commentId: String,
        override val commentContent: String,
        override val mediaProgress: UserMediaProgress,
        override val episode: Int,
        override val rating: Int
    ) : LocalUserMediaListEntry(
        id, name, episodeAmount, medium, state, commentId, commentContent, mediaProgress, episode, rating
    )
}
