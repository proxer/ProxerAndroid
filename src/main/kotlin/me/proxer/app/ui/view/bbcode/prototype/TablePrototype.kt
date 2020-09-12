package me.proxer.app.ui.view.bbcode.prototype

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.core.view.children
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object TablePrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *table( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *table *", REGEX_OPTIONS)

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = CenterPrototype.makeViews(parent, children.filter { it.prototype == TableRowPrototype }, args)

        return when (childViews.size) {
            0, 1 -> assignWeights(childViews)
            else -> listOf(
                LinearLayout(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    orientation = LinearLayout.VERTICAL

                    assignWeights(childViews).forEach { addView(it) }
                }
            )
        }
    }

    private fun assignWeights(childViews: List<View>): List<View> {
        val rows = childViews.filterIsInstance(LinearLayout::class.java).map { it to it.children.toList() }
        val maxSize = rows.map { (_, cells) -> cells.size }.maxOrNull() ?: 1
        val weight = 1f / maxSize

        rows.forEach { (row, cells) ->
            cells.forEach {
                it.layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, weight).apply {
                    gravity = Gravity.CENTER
                }
            }

            if (cells.size < maxSize) {
                val fillWeight = 1f - cells.size * weight

                row.addView(View(row.context), LinearLayout.LayoutParams(0, 0, fillWeight))
            }
        }

        return childViews
    }
}
