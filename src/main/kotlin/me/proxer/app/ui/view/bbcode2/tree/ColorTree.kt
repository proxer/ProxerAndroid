package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.support.annotation.ColorInt
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.childrenRecursiveSequence

/**
 * @author Ruben Gees
 */
class ColorTree(parent: BBTree?, children: MutableList<BBTree>, @ColorInt val color: Int) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("color", ignoreCase = true)

    override fun makeView(context: Context): View {
        return super.makeView(context).apply {
            childrenRecursiveSequence().forEach {
                if (it is TextView) it.setTextColor(color)
            }
        }
    }
}
