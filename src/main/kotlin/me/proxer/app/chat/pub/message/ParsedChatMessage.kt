package me.proxer.app.chat.pub.message

import android.text.SpannableString
import me.proxer.app.ui.view.bbcode.BBParser
import me.proxer.app.util.extension.linkify
import me.proxer.library.entity.ProxerDateItem
import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.entity.ProxerImageItem
import me.proxer.library.enums.ChatMessageAction
import me.proxer.library.enums.MessageAction
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
    val action: ChatMessageAction,
    private val date: Date
) : ProxerIdItem, ProxerImageItem, ProxerDateItem {

    @Transient
    val styledMessage = when (action) {
        MessageAction.NONE -> BBParser.parseTextOnly(message).linkify()
        else -> SpannableString("")
    }

    override fun getId() = id
    override fun getImage() = image
    override fun getDate() = date
}
