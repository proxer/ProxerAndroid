package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.support.v7.widget.AppCompatTextView
import android.view.View

/**
 * @author Ruben Gees
 */
class TextLeaf(parent: BBTree?, children: MutableList<BBTree>, private val text: String) : BBTree(parent, children) {

    override fun endsWith(code: String) = false

    override fun makeView(context: Context): View {
        val view = AppCompatTextView(context)

        view.text = text

        return view
    }
}
