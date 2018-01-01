package me.proxer.app.ui.view.bbcode.table

import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TableLayout.LayoutParams
import android.widget.TableLayout.LayoutParams.MATCH_PARENT
import android.widget.TableLayout.LayoutParams.WRAP_CONTENT
import android.widget.TableRow
import me.proxer.app.ui.view.bbcode.BBTree
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
class TableRowTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = TableRowPrototype

    override fun makeViews(context: Context): List<View> {
        val children = children.filterIsInstance(TableCellTree::class.java).flatMap { it.makeViews(context) }

        for (child in children.dropLast(1)) {
            (child.layoutParams as? MarginLayoutParams)?.apply {
                setMargins(0, 0, context.dip(4), 0)
            }
        }

        return listOf(TableRow(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            children.forEach { addView(it) }
        })
    }
}
