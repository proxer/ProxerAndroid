package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TableLayout
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
object TablePrototype : BBPrototype {

    override val startRegex = Regex(" *table( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *table *", REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.filter { it.prototype == TableRowPrototype }.flatMap { it.makeViews(context) }

        for (child in childViews.dropLast(1)) {
            (child.layoutParams as? MarginLayoutParams)?.apply {
                setMargins(0, 0, 0, context.dip(4))
            }
        }

        return listOf(TableLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            isShrinkAllColumns = true
            isStretchAllColumns = true

            childViews.forEach { addView(it) }
        })
    }
}
