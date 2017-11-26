package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.RightTree

/**
 * @author Ruben Gees
 */
object RightPrototype : BBPrototype {

    override fun fromCode(code: String, parent: BBTree) = when (code.equals("right", ignoreCase = true)) {
        true -> RightTree(parent)
        false -> null
    }
}
