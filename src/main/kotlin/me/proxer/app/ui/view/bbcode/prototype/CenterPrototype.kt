package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.CenterTree

/**
 * @author Ruben Gees
 */
object CenterPrototype : BBPrototype {

    override fun fromCode(code: String, parent: BBTree) = when (code.equals("center", ignoreCase = true)) {
        true -> CenterTree(parent)
        false -> null
    }
}
