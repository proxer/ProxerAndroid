package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.SizeTree

/**
 * @author Ruben Gees
 */
object SizePrototype : BBPrototype {

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("size", ignoreCase = true)) {
        true -> {
            val value = code.substringAfter("=").trim()

            when (value.getOrNull(0)) {
                '1' -> SizeTree(0.4f, parent)
                '2' -> SizeTree(0.6f, parent)
                '3' -> SizeTree(0.8f, parent)
                '4' -> SizeTree(1.0f, parent)
                '5' -> SizeTree(1.5f, parent)
                '6' -> SizeTree(2.0f, parent)
                else -> null
            }
        }
        false -> null
    }
}
