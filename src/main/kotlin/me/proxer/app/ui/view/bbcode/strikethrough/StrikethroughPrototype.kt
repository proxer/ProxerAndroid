package me.proxer.app.ui.view.bbcode.strikethrough

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object StrikethroughPrototype : BBPrototype {

    override val startRegex = Regex(" *(s|strike)( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *(s|strike) *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = StrikethroughTree(parent)
}
