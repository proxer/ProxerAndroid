package me.proxer.app.chat.pub.message

import me.proxer.app.ui.view.bbcode.BBParser
import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.entity.ProxerImageItem
import me.proxer.library.enums.ChatMessageAction
import java.util.Date

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
    override val date: Date
) : ProxerIdItem, ProxerImageItem, ProxerDateItem {

    @Transient
    val styledMessage = BBParser.parseSimple(message).optimize()
}
