package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree

/**
 * @author Ruben Gees
 */
interface BBPrototype {

    val startRegex: Regex
    val endRegex: Regex

    val canHaveChildren get() = true

    fun fromCode(code: String, parent: BBTree) = when (startRegex.matches(code)) {
        true -> construct(code, parent)
        false -> null
    }

    fun construct(code: String, parent: BBTree): BBTree
}
