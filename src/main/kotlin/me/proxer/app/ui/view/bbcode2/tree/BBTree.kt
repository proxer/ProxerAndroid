package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.view.View
import android.widget.LinearLayout

/**
 * @author Ruben Gees
 */
open class BBTree(val parent: BBTree?, val children: MutableList<BBTree>) {

    open fun endsWith(code: String) = false

    open fun makeView(context: Context): View {
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        children.map { it.makeView(context) }.forEach { container.addView(it) }

        return container
    }
}
