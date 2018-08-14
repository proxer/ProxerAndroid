package me.proxer.app.ui.view.bbcode.prototype

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TableLayout
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
object TablePrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *table( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *table *", REGEX_OPTIONS)

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = CenterPrototype.makeViews(parent, children.filter { it.prototype == TableRowPrototype }, args)

        for (child in childViews.dropLast(1)) {
            (child.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                setMargins(0, 0, 0, parent.dip(4))
            }
        }

        return listOf(TableLayout(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
            isShrinkAllColumns = true
            isStretchAllColumns = true

            childViews.forEach { addView(it) }
        })
    }
}
