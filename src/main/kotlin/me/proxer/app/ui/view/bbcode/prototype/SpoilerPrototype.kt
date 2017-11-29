package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.SpoilerTree

/**
 * @author Ruben Gees
 */
object SpoilerPrototype : BBPrototype {

    override val startRegex = Regex("\\s*spoiler\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*spoiler\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = SpoilerTree(parent)
}
