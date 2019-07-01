package me.proxer.app.comment

import me.proxer.app.ui.view.bbcode.toSimpleBBTree
import me.proxer.library.entity.info.RatingDetails
import me.proxer.library.enums.UserMediaProgress

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
) {
    val parsedContent = content.toSimpleBBTree()
}
