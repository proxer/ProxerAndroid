package me.proxer.app.ui.view.bbcode.left

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * @author Ruben Gees
 */
object LeftPrototype : BBPrototype {

    override val startRegex = Regex("\\s*left\\s*", IGNORE_CASE)
    override val endRegex = Regex("/\\s*left\\s*", IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = LeftTree(parent)
}
