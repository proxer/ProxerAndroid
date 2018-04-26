package me.proxer.app.chat.pub.message

import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.entity.ProxerImageItem
import me.proxer.library.enums.ChatMessageAction
import java.util.Date

/**
 * @author Ruben Gees
 */
data class ParsedChatMessage(
    private val id: String,
    val userId: String,
    val username: String,
    private val image: String,
    val message: String,
    val styledMessage: CharSequence,
    val action: ChatMessageAction,
    private val date: Date
) : ProxerIdItem, ProxerImageItem, ProxerDateItem {

    override fun getId() = id
    override fun getImage() = image
    override fun getDate() = date
}
