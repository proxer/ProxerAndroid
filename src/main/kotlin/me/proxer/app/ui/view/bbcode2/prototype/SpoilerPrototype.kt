package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.SpoilerTree

/**
 * @author Ruben Gees
 */
object SpoilerPrototype : BBPrototype() {

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("spoiler", ignoreCase = true)) {
        true -> SpoilerTree(parent)
        false -> null
    }
}
