package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.StrikethroughTree

/**
 * @author Ruben Gees
 */
object StrikethroughPrototype : BBPrototype() {

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("s", ignoreCase = true)) {
        true -> StrikethroughTree(parent)
        false -> null
    }
}
