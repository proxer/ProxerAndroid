package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object BreakPrototype : BBPrototype {

    override val startRegex = Regex(" *br *", REGEX_OPTIONS)
    override val endRegex = Regex("/ *br *", REGEX_OPTIONS)

    override val canHaveChildren get() = false

    override fun construct(code: String, parent: BBTree): BBTree {
        return TextPrototype.construct("\n", parent)
    }
}
