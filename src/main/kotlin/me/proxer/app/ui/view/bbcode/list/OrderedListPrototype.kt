package me.proxer.app.ui.view.bbcode.list

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object OrderedListPrototype : BBPrototype {

    override val startRegex = Regex(" *ol( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *ol *", BBPrototype.REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = OrderedListTree(parent)
}
