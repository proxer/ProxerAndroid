package me.proxer.app.ui.view.bbcode.image

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * @author Ruben Gees
 */
object ImagePrototype : BBPrototype {

    override val startRegex = Regex("\\s*img\\s*(size\\s*=\\s*\\d+)?\\s*", IGNORE_CASE)
    override val endRegex = Regex("/\\s*img\\s*", IGNORE_CASE)

    override fun construct(code: String, parent: BBTree): BBTree {
        val width = code
                .substringAfter("size", "").trim()
                .substringAfter("=", "").trim().trim { it == '"' }.toIntOrNull()

        return ImageTree(width, parent)
    }
}
