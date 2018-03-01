package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object ListItemPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *li( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *li *", BBPrototype.REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = super.makeViews(context, children, args)

        return when (childViews.size) {
            0, 1 -> childViews
            else -> listOf(LinearLayout(context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = VERTICAL

                childViews.forEach { addView(it) }
            })
        }
    }
}
