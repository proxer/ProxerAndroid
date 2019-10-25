package me.proxer.app.chat.pub.message

import me.proxer.app.ui.view.bbcode.toSimpleBBTree
import me.proxer.app.util.extension.toDate
import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.entity.ProxerImageItem
import me.proxer.library.enums.ChatMessageAction
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
data class ParsedChatMessage(
    override val id: String,
    val userId: String,
    val username: String,
    override val image: String,
    val message: String,
    val action: ChatMessageAction,
    val instant: Instant
) : ProxerIdItem, ProxerImageItem, ProxerDateItem {

    @Transient
    override val date = instant.toDate()

    @Transient
    val styledMessage = message.toSimpleBBTree()
}
