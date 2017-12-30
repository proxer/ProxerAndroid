package me.proxer.app.ui.view.bbcode.table

import android.content.Context
import android.view.View
import android.widget.TableLayout.LayoutParams
import android.widget.TableLayout.LayoutParams.MATCH_PARENT
import android.widget.TableLayout.LayoutParams.WRAP_CONTENT
import android.widget.TableRow
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
class TableRowTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = TableRowPrototype

    override fun makeViews(context: Context): List<View> {
        val children = children.filterIsInstance(TableCellTree::class.java).flatMap { it.makeViews(context) }

        return listOf(TableRow(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            children.forEach { addView(it) }
        })
    }
}
