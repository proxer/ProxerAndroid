package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import org.jetbrains.anko.dip

object QuotePrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *quote( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *quote *", REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = super.makeViews(context, children, args)

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
