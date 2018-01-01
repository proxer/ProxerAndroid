package me.proxer.app.forum

import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import java.util.Date

/**
 * @author Ruben Gees
 */
data class ParsedPost(
        private val id: String,
        val parentId: String,
        val userId: String,
        val username: String,
        private val date: Date,
        val modifiedById: String?,
        val modifiedByName: String?,
        val modifiedReason: String?,
        val parsedMessage: BBTree
) : ProxerIdItem, ProxerDateItem {

    override fun getId() = id
    override fun getDate() = date
}
