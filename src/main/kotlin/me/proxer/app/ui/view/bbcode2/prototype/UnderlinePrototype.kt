package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.UnderlineTree

/**
 * @author Ruben Gees
 */
object UnderlinePrototype : BBPrototype() {

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("u", ignoreCase = true)) {
        true -> UnderlineTree(parent)
        false -> null
    }
}
