package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TableLayout.LayoutParams
import android.widget.TableLayout.LayoutParams.MATCH_PARENT
import android.widget.TableLayout.LayoutParams.WRAP_CONTENT
import android.widget.TableRow
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
object TableRowPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *tr( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *tr *", REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.filter { it.prototype == TableCellPrototype }.flatMap { it.makeViews(context) }

        for (child in childViews.dropLast(1)) {
            (child.layoutParams as? MarginLayoutParams)?.apply {
                setMargins(0, 0, context.dip(4), 0)
            }
        }

        return listOf(TableRow(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            childViews.forEach { addView(it) }
        })
    }
}
