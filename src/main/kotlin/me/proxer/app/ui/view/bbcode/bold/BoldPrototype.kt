package me.proxer.app.ui.view.bbcode.bold

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * @author Ruben Gees
 */
object BoldPrototype : BBPrototype {

    override val startRegex = Regex("\\s*b\\s*", IGNORE_CASE)
    override val endRegex = Regex("/\\s*b\\s*", IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = BoldTree(parent)
}
