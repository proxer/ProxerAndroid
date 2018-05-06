package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.graphics.Typeface.MONOSPACE
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.FrameLayout
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
object CodePrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *code( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *code *", BBPrototype.REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = super.makeViews(context, children, args)

        applyToViews(childViews, { view: TextView ->
            view.typeface = MONOSPACE
        })

        return when (childViews.size) {
            0 -> childViews
            1 -> listOf(FrameLayout(context).apply {
                val fourDip = dip(4)

                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                setPadding(fourDip, fourDip, fourDip, fourDip)
                setBackgroundColor(ContextCompat.getColor(context, R.color.selected))

                childViews.forEach { addView(it) }
            })
            else -> listOf(LinearLayout(context).apply {
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
