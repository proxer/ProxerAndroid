package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.LeftTree

/**
 * @author Ruben Gees
 */
object LeftPrototype : BBPrototype {

    override val startRegex = Regex("\\s*left\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*left\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = LeftTree(parent)
}
