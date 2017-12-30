package me.proxer.app.ui.view.bbcode.strikethrough

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object StrikethroughPrototype : BBPrototype {

    override val startRegex = Regex("\\s*(s|strike)\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*(s|string)\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = StrikethroughTree(parent)
}
