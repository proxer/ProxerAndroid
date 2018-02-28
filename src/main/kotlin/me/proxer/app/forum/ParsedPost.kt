package me.proxer.app.forum

import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.entity.ProxerImageItem
import java.util.Date

/**
 * @author Ruben Gees
 */
data class ParsedPost(
    private val id: String,
    val parentId: String,
    val userId: String,
    val username: String,
    private val image: String,
    private val date: Date,
    val signature: BBTree?,
    val modifiedById: String?,
    val modifiedByName: String?,
    val modifiedReason: String?,
    val parsedMessage: BBTree,
    val thankYouAmount: Int
) : ProxerIdItem, ProxerImageItem, ProxerDateItem {

    override fun getId() = id
    override fun getImage() = image
    override fun getDate() = date
}
