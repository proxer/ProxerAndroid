package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.BoldTree

/**
 * @author Ruben Gees
 */
object BoldPrototype : BBPrototype {

    override val startRegex = Regex("\\s*b\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*b\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = BoldTree(parent)
}
