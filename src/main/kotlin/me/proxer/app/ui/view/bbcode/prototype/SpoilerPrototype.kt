package me.proxer.app.ui.view.bbcode.prototype

import android.view.View
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBSpoilerView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object SpoilerPrototype : AutoClosingPrototype {

    const val SPOILER_TEXT_COLOR_ARGUMENT = "spoiler_text_color"
    const val SPOILER_EXPAND_ARGUMENT = "spoiler_expand"

    private const val TITLE_ARGUMENT = "title"

    private val attributeRegex = Regex("spoiler *= *(.+?)$", REGEX_OPTIONS)

    override val startRegex = Regex(" *spoiler( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *spoiler *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val title = BBUtils.cutAttribute(code, attributeRegex)

        return BBTree(this, parent, args = BBArgs(custom = arrayOf(TITLE_ARGUMENT to title)))
    }

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = super.makeViews(parent, children, args)
        val title = args[TITLE_ARGUMENT] as String?
        val shouldExpand = args[SPOILER_EXPAND_ARGUMENT] as Boolean? ?: false

        return when (childViews.isEmpty()) {
            true -> childViews
            false -> listOf(
                BBSpoilerView(parent.context).apply {
                    (args[SPOILER_TEXT_COLOR_ARGUMENT] as? Int)?.let { spoilerTextColor = it }

                    spoilerTitle = title
                    isExpanded = shouldExpand

                    childViews.forEach { addView(it) }
                }
            )
        }
    }
}
