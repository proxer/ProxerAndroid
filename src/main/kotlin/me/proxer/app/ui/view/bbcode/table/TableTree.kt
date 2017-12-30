package me.proxer.app.ui.view.bbcode.table

import android.content.Context
import android.view.View
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TableLayout
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
class TableTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = TablePrototype

    override fun makeViews(context: Context): List<View> {
        val children = children.filterIsInstance(TableRowTree::class.java).flatMap { it.makeViews(context) }

        return listOf(TableLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            isShrinkAllColumns = true
            isStretchAllColumns = true

            children.forEach { addView(it) }
        })
    }
}
