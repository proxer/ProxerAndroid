package me.proxer.app.ui.view.bbcode.strikethrough

import android.content.Context
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder

/**
 * @author Ruben Gees
 */
class StrikethroughTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = StrikethroughPrototype

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViewsWithoutMerging(context)

        return applyToViews(childViews) { view: TextView ->
            view.text = view.text.toSpannableStringBuilder().apply {
                setSpan(StrikethroughSpan(), 0, view.length(), SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}