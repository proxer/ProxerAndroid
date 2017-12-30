package me.proxer.app.ui.view.bbcode.spoiler

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * @author Ruben Gees
 */
object SpoilerPrototype : BBPrototype {

    override val startRegex = Regex("\\s*spoiler\\s*(=\\s*\"?.*?\"?\\s*)?", IGNORE_CASE)
    override val endRegex = Regex("/\\s*spoiler\\s*", IGNORE_CASE)

    override fun construct(code: String, parent: BBTree): SpoilerTree {
        val title = code.substringAfter("=", "").trim().trim { it == '"' }
        val parsedTitle = if (title.isBlank()) null else title

        return SpoilerTree(parsedTitle, parent)
    }
}
