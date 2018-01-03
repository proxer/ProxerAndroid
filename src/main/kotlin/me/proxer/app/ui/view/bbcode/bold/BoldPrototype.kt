package me.proxer.app.ui.view.bbcode.bold

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object BoldPrototype : BBPrototype {

    override val startRegex = Regex(" *b( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *b *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = BoldTree(parent)
}
