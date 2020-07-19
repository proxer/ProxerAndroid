package me.proxer.app.ui.view.bbcode.prototype

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object ListItemPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *li( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *li *", BBPrototype.REGEX_OPTIONS)

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = super.makeViews(parent, children, args)

        return when (childViews.size) {
            0, 1 -> childViews
            else -> listOf(
                LinearLayout(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    orientation = VERTICAL

                    childViews.forEach { addView(it) }
                }
            )
        }
    }
}
