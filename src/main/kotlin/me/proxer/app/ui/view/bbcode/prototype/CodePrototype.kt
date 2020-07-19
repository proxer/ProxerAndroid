package me.proxer.app.ui.view.bbcode.prototype

import android.graphics.Typeface.MONOSPACE
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.resolveColor

/**
 * @author Ruben Gees
 */
object CodePrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *code( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *code *", BBPrototype.REGEX_OPTIONS)

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = super.makeViews(parent, children, args)

        applyToViews(childViews) { view: TextView ->
            view.typeface = MONOSPACE
        }

        return when (childViews.size) {
            0 -> childViews
            1 -> listOf(
                FrameLayout(parent.context).apply {
                    val fourDip = dip(4)

                    layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)

                    setPadding(fourDip, fourDip, fourDip, fourDip)
                    setBackgroundColor(parent.context.resolveColor(R.attr.colorSelectedSurface))

                    childViews.forEach { addView(it) }
                }
            )
            else -> listOf(
                LinearLayout(parent.context).apply {
                    val fourDip = dip(4)

                    layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    orientation = VERTICAL

                    setPadding(fourDip, fourDip, fourDip, fourDip)
                    setBackgroundColor(parent.context.resolveColor(R.attr.colorSelectedSurface))

                    childViews.forEach { addView(it) }
                }
            )
        }
    }
}
