package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TableRow.LayoutParams
import android.widget.TableRow.LayoutParams.WRAP_CONTENT
import android.widget.TableRow.VERTICAL
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object TableCellPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *td( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *td *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = BBTree(this, parent)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = super.makeViews(context, children, args)

        return when (childViews.size) {
            0, 1 -> childViews.map {
                it.apply { layoutParams = LayoutParams(0, WRAP_CONTENT, 1f) }
            }
            else -> listOf(LinearLayout(context).apply {
                layoutParams = LayoutParams(0, WRAP_CONTENT, 1f)
                orientation = VERTICAL

                childViews.forEach { addView(it) }
            })
        }
    }
}
