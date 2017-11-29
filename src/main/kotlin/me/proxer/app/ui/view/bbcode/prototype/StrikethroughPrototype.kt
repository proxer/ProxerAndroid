package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.StrikethroughTree

/**
 * @author Ruben Gees
 */
object StrikethroughPrototype : BBPrototype {

    override val startRegex = Regex("\\s*s\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*s\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = StrikethroughTree(parent)
}
