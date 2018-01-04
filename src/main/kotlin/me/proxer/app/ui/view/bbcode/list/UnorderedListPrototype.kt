package me.proxer.app.ui.view.bbcode.list

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object UnorderedListPrototype : BBPrototype {

    override val startRegex = Regex(" *ul( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *ul *", BBPrototype.REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = UnorderedListTree(parent)
}
