package me.proxer.app.ui.view.bbcode.italic

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object ItalicPrototype : BBPrototype {

    override val startRegex = Regex(" *i( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *i *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = ItalicTree(parent)
}
