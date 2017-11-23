package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.RightTree

/**
 * @author Ruben Gees
 */
object RightPrototype : BBPrototype() {

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("right", ignoreCase = true)) {
        true -> RightTree(parent, mutableListOf())
        false -> null
    }
}
