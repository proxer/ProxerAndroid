package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.ItalicTree

/**
 * @author Ruben Gees
 */
object ItalicPrototype : BBPrototype() {

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("i", ignoreCase = true)) {
        true -> ItalicTree(parent, mutableListOf())
        false -> null
    }
}
