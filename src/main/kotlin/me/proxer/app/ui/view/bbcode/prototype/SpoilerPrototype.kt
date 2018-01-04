package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.View
import me.proxer.app.ui.view.bbcode.BBSpoilerView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object SpoilerPrototype : BBPrototype {

    private const val DELIMITER = "spoiler="
    private const val TITLE_ARGUMENT = "title"

    override val startRegex = Regex(" *spoiler(=\"?.*?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *spoiler *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val titleIndex = code.indexOf(DELIMITER, ignoreCase = true)

        val title = when (titleIndex < 0) {
            true -> null
            false -> code.substring(titleIndex + DELIMITER.length, code.length).trim().trim { it == '"' }
        }

        return BBTree(this, parent, args = mapOf(TITLE_ARGUMENT to title))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = super.makeViews(context, children, args)
        val title = args[TITLE_ARGUMENT] as String?

        return when (childViews.isEmpty()) {
            true -> childViews
            false -> listOf(BBSpoilerView(context).apply {
                spoilerTitle = title

                childViews.forEach { addView(it) }
            })
        }
    }
}
