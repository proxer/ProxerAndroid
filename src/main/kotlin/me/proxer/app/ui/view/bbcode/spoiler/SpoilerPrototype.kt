package me.proxer.app.ui.view.bbcode.spoiler

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object SpoilerPrototype : BBPrototype {

    private const val DELIMITER = "spoiler="

    override val startRegex = Regex(" *spoiler(=\"?.*?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *spoiler *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): SpoilerTree {
        val titleIndex = code.indexOf(DELIMITER, ignoreCase = true)

        val title = when (titleIndex < 0) {
            true -> null
            false -> code.substring(titleIndex + DELIMITER.length, code.length).trim().trim { it == '"' }
        }

        return SpoilerTree(title, parent)
    }
}
