package me.proxer.app.ui.view.bbcode.table

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TableRow.LayoutParams
import android.widget.TableRow.LayoutParams.WRAP_CONTENT
import android.widget.TableRow.VERTICAL
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
class TableCellTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = TableCellPrototype

    override fun makeViews(context: Context): List<View> {
        val children = super.makeViews(context)

        return when (children.size) {
            0, 1 -> children.map {
                when (it.layoutParams?.width ?: -1 > 0 || it.layoutParams?.height ?: -1 > 0) {
                    true -> FrameLayout(context).apply {
                        layoutParams = LayoutParams(0, WRAP_CONTENT, 1f)

                        addView(it)
                    }
                    false -> it.apply { layoutParams = LayoutParams(0, WRAP_CONTENT, 1f) }
                }
            }
            else -> listOf(LinearLayout(context).apply {
                layoutParams = LayoutParams(0, WRAP_CONTENT, 1f)
                orientation = VERTICAL

                children.forEach { addView(it) }
            })
        }
    }
}
