package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.SizeTree

/**
 * @author Ruben Gees
 */
object SizePrototype : BBPrototype() {

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("size", ignoreCase = true)) {
        true -> {
            val value = code.substringAfter("=").trim()

            when (value.getOrNull(0)) {
                '1' -> SizeTree(parent, mutableListOf(), 0.4f)
                '2' -> SizeTree(parent, mutableListOf(), 0.6f)
                '3' -> SizeTree(parent, mutableListOf(), 0.8f)
                '4' -> SizeTree(parent, mutableListOf(), 1.0f)
                '5' -> SizeTree(parent, mutableListOf(), 1.5f)
                '6' -> SizeTree(parent, mutableListOf(), 2.0f)
                else -> null
            }
        }
        false -> null
    }
}
