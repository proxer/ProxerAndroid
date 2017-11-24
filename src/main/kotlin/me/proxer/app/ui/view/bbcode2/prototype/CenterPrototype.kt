package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.CenterTree

/**
 * @author Ruben Gees
 */
object CenterPrototype : BBPrototype() {

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("center", ignoreCase = true)) {
        true -> CenterTree(parent)
        false -> null
    }
}
