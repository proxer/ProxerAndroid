package me.proxer.app.ui.view.bbcode.size

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils

/**
 * @author Ruben Gees
 */
object SizePrototype : BBPrototype {

    override val startRegex = Regex(" *size=\"?[1-6]\"?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *size *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val value = BBUtils.cutAttribute(code, "size=")

        return when (value) {
            "1" -> SizeTree(0.4f, parent)
            "2" -> SizeTree(0.7f, parent)
            "3" -> SizeTree(0.85f, parent)
            "4" -> SizeTree(1.0f, parent)
            "5" -> SizeTree(1.5f, parent)
            "6" -> SizeTree(2.0f, parent)
            else -> throw IllegalArgumentException("Unknown size: $value")
        }
    }
}
