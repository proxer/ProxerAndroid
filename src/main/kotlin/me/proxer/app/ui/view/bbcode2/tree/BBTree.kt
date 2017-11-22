package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.view.View
import android.widget.TextView

/**
 * @author Ruben Gees
 */
open class BBTree(val parent: BBTree?, val children: MutableList<BBTree>) {

    open fun endsWith(code: String) = false

    open fun makeViews(context: Context): List<View> {
        val views = children.flatMap { it.makeViews(context) }

        if (views.size <= 1) {
            return views
        } else {
            val result = mutableListOf<View>()
            var previous = views.first()

            for (current in views.drop(1)) {
                if (previous is TextView && current is TextView) {
                    previous.append(current.text)
                } else {
                    result += (previous)

                    previous = current
                }
            }

            result += previous

            return result
        }
    }
}
