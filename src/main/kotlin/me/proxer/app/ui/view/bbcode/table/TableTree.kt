package me.proxer.app.ui.view.bbcode.table

import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TableLayout
import me.proxer.app.ui.view.bbcode.BBTree
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
class TableTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = TablePrototype

    override fun makeViews(context: Context): List<View> {
        val children = children.filterIsInstance(TableRowTree::class.java).flatMap { it.makeViews(context) }

        for (child in children.dropLast(1)) {
            (child.layoutParams as? MarginLayoutParams)?.apply {
                setMargins(0, 0, 0, context.dip(4))
            }
        }

        return listOf(TableLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            isShrinkAllColumns = true
            isStretchAllColumns = true

            children.forEach { addView(it) }
        })
    }
}
