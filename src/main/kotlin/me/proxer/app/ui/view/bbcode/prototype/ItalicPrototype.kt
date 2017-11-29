package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.ItalicTree

/**
 * @author Ruben Gees
 */
object ItalicPrototype : BBPrototype {

    override val startRegex = Regex("\\s*i\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*i\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = ItalicTree(parent)
}
