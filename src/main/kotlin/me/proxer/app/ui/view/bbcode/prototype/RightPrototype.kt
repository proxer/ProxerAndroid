package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.RightTree

/**
 * @author Ruben Gees
 */
object RightPrototype : BBPrototype {

    override val startRegex = Regex("\\s*right\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*right\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = RightTree(parent)
}
