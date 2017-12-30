package me.proxer.app.ui.view.bbcode.divider

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.LinearLayout.LayoutParams
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
class DividerTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = DividerPrototype

    override fun makeViews(context: Context): List<View> {
        return listOf(View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dip(2))

            setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
        })
    }
}
