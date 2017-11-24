package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.view.View
import me.proxer.app.ui.view.bbcode2.BBSpoilerView

/**
 * @author Ruben Gees
 */
class SpoilerTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("spoiler", ignoreCase = true)

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViewsWithoutMerging(context)

        return listOf(BBSpoilerView(context).apply { childViews.forEach { addView(it) } })
    }
}
