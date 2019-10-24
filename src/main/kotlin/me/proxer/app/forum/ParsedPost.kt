package me.proxer.app.forum

import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.util.extension.toDate
import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.entity.ProxerImageItem
import java.time.Instant

/**
 * @author Ruben Gees
 */
data class ParsedPost(
    override val id: String,
    val parentId: String,
    val userId: String,
    val username: String,
    override val image: String,
    val instant: Instant,
    val signature: BBTree?,
    val modifiedById: String?,
    val modifiedByName: String?,
    val modifiedReason: String?,
    val parsedMessage: BBTree,
    val thankYouAmount: Int
) : ProxerIdItem, ProxerImageItem, ProxerDateItem {

    @Transient
    override val date = instant.toDate()
}
