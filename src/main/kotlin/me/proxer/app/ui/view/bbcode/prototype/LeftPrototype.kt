package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.LeftTree

/**
 * @author Ruben Gees
 */
object LeftPrototype : BBPrototype {

    override fun fromCode(code: String, parent: BBTree) = when (code.equals("left", ignoreCase = true)) {
        true -> LeftTree(parent)
        false -> null
    }
}
