package me.proxer.app.ui.view.bbcode.code

import android.content.Context
import android.graphics.Typeface.MONOSPACE
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
class CodeTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = CodePrototype

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViews(context)

        applyToViews(childViews, { view: TextView ->
            view.typeface = MONOSPACE
        })

        return when (childViews.isEmpty()) {
            true -> childViews
            false -> listOf(LinearLayout(context).apply {
                val fourDip = dip(4)

                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = VERTICAL

                setPadding(fourDip, fourDip, fourDip, fourDip)
                setBackgroundColor(ContextCompat.getColor(context, R.color.selected))

                childViews.forEach { addView(it) }
            })
        }
    }
}
