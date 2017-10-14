package me.proxer.app.log

import me.proxer.app.util.Utils
import org.threeten.bp.LocalDateTime

/**
 * @author Ruben Gees
 */
data class LogMessage(val content: String, val dateTime: LocalDateTime) {
    override fun toString() = "${Utils.dateTimeFormatter.format(dateTime)}: $content"
}
