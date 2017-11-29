package me.proxer.app.ui.view.bbcode.tree

import android.content.Context
import android.text.Layout.Alignment.ALIGN_OPPOSITE
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AlignmentSpan
import android.view.View
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.RightPrototype

/**
 * @author Ruben Gees
 */
class RightTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = RightPrototype

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViewsWithoutMerging(context)

        return BBUtils.applyToTextViews(childViews) { view ->
            view.text = SpannableStringBuilder(view.text).apply {
                setSpan(AlignmentSpan.Standard(ALIGN_OPPOSITE), 0, view.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
