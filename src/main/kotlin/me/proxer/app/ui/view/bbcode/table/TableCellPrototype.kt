package me.proxer.app.ui.view.bbcode.table

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object TableCellPrototype : BBPrototype {

    override val startRegex = Regex(" *td( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *td *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = TableCellTree(parent)
}
