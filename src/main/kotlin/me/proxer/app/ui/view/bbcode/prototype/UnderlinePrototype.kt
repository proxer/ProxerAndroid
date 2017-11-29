package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.UnderlineTree

/**
 * @author Ruben Gees
 */
object UnderlinePrototype : BBPrototype {

    override val startRegex = Regex("\\s*u\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*u\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = UnderlineTree(parent)
}
