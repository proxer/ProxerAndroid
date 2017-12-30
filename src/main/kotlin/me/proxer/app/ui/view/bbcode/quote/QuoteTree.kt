package me.proxer.app.ui.view.bbcode.quote

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree
import org.jetbrains.anko.dip

class QuoteTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = QuotePrototype

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViews(context)

        return when (childViews.isEmpty()) {
            true -> childViews
            false -> listOf(LinearLayout(context).apply {
                val fourDip = dip(4)

                orientation = VERTICAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                setPadding(fourDip, fourDip, fourDip, fourDip)
                setBackgroundColor(ContextCompat.getColor(context, R.color.selected))

                childViews.forEach { addView(it) }
            })
        }
    }
}
