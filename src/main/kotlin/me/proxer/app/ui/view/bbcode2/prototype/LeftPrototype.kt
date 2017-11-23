package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.LeftTree

/**
 * @author Ruben Gees
 */
object LeftPrototype : BBPrototype() {

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("left", ignoreCase = true)) {
        true -> LeftTree(parent, mutableListOf())
        false -> null
    }
}
