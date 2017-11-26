package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.UnderlineTree

/**
 * @author Ruben Gees
 */
object UnderlinePrototype : BBPrototype {

    override fun fromCode(code: String, parent: BBTree) = when (code.equals("u", ignoreCase = true)) {
        true -> UnderlineTree(parent)
        false -> null
    }
}
