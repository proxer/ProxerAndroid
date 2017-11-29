package me.proxer.app.ui.view.bbcode.tree

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.View
import me.proxer.app.ui.view.bbcode.BBUtils.applyToTextViews
import me.proxer.app.ui.view.bbcode.prototype.SizePrototype

/**
 * @author Ruben Gees
 */
class SizeTree(
        private val relativeSize: Float,
        parent: BBTree?,
        children: MutableList<BBTree> = mutableListOf()
) : BBTree(parent, children) {

    override val prototype = SizePrototype

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViewsWithoutMerging(context)

        return applyToTextViews(childViews) { view ->
            view.text = SpannableStringBuilder(view.text).apply {
                setSpan(RelativeSizeSpan(relativeSize), 0, view.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
