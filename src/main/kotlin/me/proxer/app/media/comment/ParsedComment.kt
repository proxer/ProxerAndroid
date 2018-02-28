package me.proxer.app.media.comment

import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.entity.ProxerImageItem
import me.proxer.library.entity.info.RatingDetails
import me.proxer.library.enums.UserMediaProgress
import java.util.*

/**
 * @author Ruben Gees
 */
data class ParsedComment(
        private val id: String,
        val entryId: String,
        val authorId: String,
        val mediaProgress: UserMediaProgress,
        val ratingDetails: RatingDetails,
        val parsedContent: BBTree,
        val overallRating: Int,
        val episode: Int,
        val helpfulVotes: Int,
        private val date: Date,
        val author: String,
        private val image: String
) : ProxerIdItem, ProxerImageItem, ProxerDateItem {

    override fun getId() = id
    override fun getImage() = image
    override fun getDate() = date
}
