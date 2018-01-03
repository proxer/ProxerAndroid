package me.proxer.app.ui.view.bbcode.right

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object RightPrototype : BBPrototype {

    override val startRegex = Regex(" *right( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *right *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = RightTree(parent)
}
