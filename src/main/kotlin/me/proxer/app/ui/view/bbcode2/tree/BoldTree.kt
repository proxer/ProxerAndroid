package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.childrenRecursiveSequence

/**
 * @author Ruben Gees
 */
class BoldTree(parent: BBTree?, children: MutableList<BBTree>) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("b", ignoreCase = true)

    override fun makeView(context: Context): View {
        return super.makeView(context).apply {
            childrenRecursiveSequence().forEach {
                if (it is TextView) it.setTypeface(it.typeface, Typeface.BOLD)
            }
        }
    }
}
