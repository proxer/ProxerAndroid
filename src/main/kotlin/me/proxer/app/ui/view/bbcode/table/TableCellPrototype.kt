package me.proxer.app.ui.view.bbcode.table

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * @author Ruben Gees
 */
object TableCellPrototype : BBPrototype {

    override val startRegex = Regex("\\s*td\\s*", IGNORE_CASE)
    override val endRegex = Regex("/\\s*td\\s*", IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = TableCellTree(parent)
}
