package me.proxer.app.ui.view.bbcode.tree

import android.content.Context
import android.view.View
import me.proxer.app.ui.view.bbcode.BBSpoilerView

/**
 * @author Ruben Gees
 */
class SpoilerTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.equals("spoiler", ignoreCase = true)

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViews(context)

        return listOf(BBSpoilerView(context).apply { childViews.forEach { addView(it) } })
    }
}
