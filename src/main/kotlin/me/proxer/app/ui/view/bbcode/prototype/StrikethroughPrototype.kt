package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.StrikethroughTree

/**
 * @author Ruben Gees
 */
object StrikethroughPrototype : BBPrototype {

    override fun fromCode(code: String, parent: BBTree) = when (code.equals("s", ignoreCase = true)) {
        true -> StrikethroughTree(parent)
        false -> null
    }
}
