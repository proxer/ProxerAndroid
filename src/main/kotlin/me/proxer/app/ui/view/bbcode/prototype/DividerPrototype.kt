package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.DividerTree

/**
 * @author Ruben Gees
 */
object DividerPrototype : BBPrototype {

    override val startRegex = Regex("\\s*hr\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex(".*", RegexOption.DOT_MATCHES_ALL)

    override val canHaveChildren get() = false

    override fun construct(code: String, parent: BBTree) = DividerTree(parent)
}
