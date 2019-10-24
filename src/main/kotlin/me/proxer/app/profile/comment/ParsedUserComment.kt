package me.proxer.app.profile.comment

import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.util.extension.toDate
import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.entity.ProxerImageItem
import me.proxer.library.entity.info.RatingDetails
import me.proxer.library.enums.Category
import me.proxer.library.enums.Medium
import me.proxer.library.enums.UserMediaProgress
import java.time.Instant

/**
 * @author Ruben Gees
 */
data class ParsedUserComment(
    override val id: String,
    val entryId: String,
    val entryName: String,
    val medium: Medium,
    val category: Category,
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
