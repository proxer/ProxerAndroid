package me.proxer.app.ui.view.bbcode.tree

import android.content.Context
import android.view.View
import android.widget.TextView

/**
 * @author Ruben Gees
 */
open class BBTree(val parent: BBTree?, val children: MutableList<BBTree> = mutableListOf()) {

    open fun endsWith(code: String) = false

    open fun makeViews(context: Context): List<View> {
        val views = children.flatMap { it.makeViews(context) }

        if (views.size <= 1) {
            return views
        } else {
            val result = mutableListOf<View>()
            var current = views.first()

            if (current is TextView) {
                current.text = current.text.trimStart()
            }

            for (next in views.drop(1)) {
                if (current is TextView && next is TextView) {
                    current.append(next.text)
                } else {
                    result += current

                    current = next
                }
            }

            if (current is TextView) {
                current.text = current.text.trimEnd()
            }

            result += current

            return result
        }
    }

    protected fun makeViewsWithoutMerging(context: Context) = children.flatMap { it.makeViews(context) }
}
