package me.proxer.app.ui.view.bbcode.list

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
class ListItemTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = ListItemPrototype

    override fun makeViews(context: Context): List<View> {
        val children = super.makeViews(context)

        return when (children.size) {
            0, 1 -> children
            else -> listOf(LinearLayout(context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = VERTICAL

                children.forEach { addView(it) }
            })
        }
    }
}
