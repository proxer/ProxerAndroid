package me.proxer.app.media.comment

import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.entity.ProxerImageItem
import me.proxer.library.entity.info.RatingDetails
import me.proxer.library.enums.UserMediaProgress
import java.util.Date

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
    override val date: Date,
    val author: String,
    override val image: String
) : ProxerIdItem, ProxerImageItem, ProxerDateItem
