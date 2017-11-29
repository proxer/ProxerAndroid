package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.CenterTree

/**
 * @author Ruben Gees
 */
object CenterPrototype : BBPrototype {

    override val startRegex = Regex("\\s*center\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*center\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = CenterTree(parent)
}
