package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.view.Gravity
import android.view.View
import me.proxer.app.ui.view.bbcode2.BBUtils

/**
 * @author Ruben Gees
 */
class CenterTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("center", ignoreCase = true)

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViewsWithoutMerging(context)

        return BBUtils.applyToTextViews(childViews) { view ->
            view.gravity = Gravity.CENTER_HORIZONTAL
        }
    }
}
