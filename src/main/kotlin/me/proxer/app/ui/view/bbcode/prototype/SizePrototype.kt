package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.SizeTree

/**
 * @author Ruben Gees
 */
object SizePrototype : BBPrototype {

    override val startRegex = Regex("\\s*size\\s*=\\s*[1-6]\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*size\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree): BBTree {
        val value = code.substringAfter("=").trim().getOrNull(0)

        return when (value) {
            '1' -> SizeTree(0.4f, parent)
            '2' -> SizeTree(0.6f, parent)
            '3' -> SizeTree(0.8f, parent)
            '4' -> SizeTree(1.0f, parent)
            '5' -> SizeTree(1.5f, parent)
            '6' -> SizeTree(2.0f, parent)
            else -> throw IllegalArgumentException("Unknown size: $value")
        }
    }
}
