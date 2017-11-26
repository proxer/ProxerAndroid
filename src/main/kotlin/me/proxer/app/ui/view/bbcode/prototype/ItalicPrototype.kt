package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.ItalicTree

/**
 * @author Ruben Gees
 */
object ItalicPrototype : BBPrototype {

    override fun fromCode(code: String, parent: BBTree) = when (code.equals("i", ignoreCase = true)) {
        true -> ItalicTree(parent)
        false -> null
    }
}
