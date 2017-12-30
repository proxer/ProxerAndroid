package me.proxer.app.ui.view.bbcode.divider

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object DividerPrototype : BBPrototype {

    override val startRegex = Regex("\\s*hr\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*hr\\s*", RegexOption.DOT_MATCHES_ALL)

    override val canHaveChildren get() = false

    override fun construct(code: String, parent: BBTree) = DividerTree(parent)
}
