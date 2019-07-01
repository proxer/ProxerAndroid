package me.proxer.app.media.comments

import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.util.extension.toDate
import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.entity.ProxerImageItem
import me.proxer.library.entity.info.RatingDetails
import me.proxer.library.enums.UserMediaProgress
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
data class ParsedComment(
    override val id: String,
    val entryId: String,
    val authorId: String,
    val mediaProgress: UserMediaProgress,
    val ratingDetails: RatingDetails,
    val parsedContent: BBTree,
    val overallRating: Int,
    val episode: Int,
    val helpfulVotes: Int,
    val instant: Instant,
    val author: String,
    override val image: String
) : ProxerIdItem, ProxerImageItem, ProxerDateItem {

    @Transient
    override val date = instant.toDate()
}
