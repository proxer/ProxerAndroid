package me.proxer.app.ui.view.bbcode.prototype

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TableRow
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.extension.dip

/**
 * @author Ruben Gees
 */
object TableRowPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *tr( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *tr *", REGEX_OPTIONS)

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.filter { it.prototype == TableCellPrototype }.flatMap { it.makeViews(parent, args) }

        childViews.dropLast(1)
            .asSequence()
            .map { it to it.layoutParams as? ViewGroup.MarginLayoutParams? }
            .filterNot { (_, layoutParams) -> layoutParams == null }
            .toList()
            .forEach { (child, layoutParams) ->
                child.layoutParams = layoutParams?.apply {
                    setMargins(0, 0, 0, parent.dip(4))
                }
            }

        return listOf(TableRow(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)

            childViews.forEach { addView(it) }
        })
    }
}
