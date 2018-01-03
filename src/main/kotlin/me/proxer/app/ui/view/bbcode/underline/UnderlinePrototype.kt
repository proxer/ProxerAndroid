package me.proxer.app.ui.view.bbcode.underline

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object UnderlinePrototype : BBPrototype {

    override val startRegex = Regex(" *u( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *u *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = UnderlineTree(parent)
}
