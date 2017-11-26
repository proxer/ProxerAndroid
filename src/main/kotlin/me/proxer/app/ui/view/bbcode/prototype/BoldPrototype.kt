package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.BoldTree

/**
 * @author Ruben Gees
 */
object BoldPrototype : BBPrototype {

    override fun fromCode(code: String, parent: BBTree) = when (code.equals("b", ignoreCase = true)) {
        true -> BoldTree(parent)
        false -> null
    }
}
