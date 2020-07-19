package me.proxer.app.ui.view.bbcode.prototype

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object TableRowPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *tr( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *tr *", REGEX_OPTIONS)

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.filter { it.prototype == TableCellPrototype }.flatMap { it.makeViews(parent, args) }

        return when (childViews.size) {
            0 -> emptyList()
            else -> listOf(
                LinearLayout(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    orientation = HORIZONTAL

                    childViews.forEach { addView(it) }
                }
            )
        }
    }
}
