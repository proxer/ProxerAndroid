package me.proxer.app.ui.view.bbcode.divider

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object DividerPrototype : BBPrototype {

    override val startRegex = Regex(" *hr *", REGEX_OPTIONS)
    override val endRegex = Regex("/ *hr *", REGEX_OPTIONS)

    override val canHaveChildren get() = false

    override fun construct(code: String, parent: BBTree) = DividerTree(parent)
}
