package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.SpoilerTree

/**
 * @author Ruben Gees
 */
object SpoilerPrototype : BBPrototype {

    override fun fromCode(code: String, parent: BBTree) = when (code.equals("spoiler", ignoreCase = true)) {
        true -> SpoilerTree(parent)
        false -> null
    }
}
