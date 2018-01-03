package me.proxer.app.ui.view.bbcode.table

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object TablePrototype : BBPrototype {

    override val startRegex = Regex(" *table( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *table *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = TableTree(parent)
}
