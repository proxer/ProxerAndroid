package me.proxer.app.ui.view.bbcode.italic

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object ItalicPrototype : BBPrototype {

    override val startRegex = Regex("\\s*i\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*i\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = ItalicTree(parent)
}
