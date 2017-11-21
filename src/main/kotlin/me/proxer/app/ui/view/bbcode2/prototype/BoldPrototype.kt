package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.BoldTree

/**
 * @author Ruben Gees
 */
object BoldPrototype : BBPrototype() {

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("b", ignoreCase = true)) {
        true -> BoldTree(parent, mutableListOf())
        false -> null
    }
}
