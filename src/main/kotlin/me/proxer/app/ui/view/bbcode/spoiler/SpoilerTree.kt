package me.proxer.app.ui.view.bbcode.spoiler

import android.content.Context
import android.view.View
import me.proxer.app.ui.view.bbcode.BBSpoilerView
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
class SpoilerTree(
        private val title: String?,
        parent: BBTree?,
        children: MutableList<BBTree> = mutableListOf()
) : BBTree(parent, children) {

    override val prototype = SpoilerPrototype

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViews(context)

        return when (childViews.isEmpty()) {
            true -> childViews
            false -> listOf(BBSpoilerView(context).apply {
                spoilerTitle = title

                childViews.forEach { addView(it) }
            })
        }
    }
}
