package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.view.Gravity
import me.proxer.app.ui.view.bbcode2.BBUtils

/**
 * @author Ruben Gees
 */
class RightTree(parent: BBTree?, children: MutableList<BBTree>) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("right", ignoreCase = true)

    override fun makeViews(context: Context) = BBUtils.applyToTextViews(super.makeViewsWithoutMerging(context)) { view ->
        view.gravity = Gravity.END
    }
}
