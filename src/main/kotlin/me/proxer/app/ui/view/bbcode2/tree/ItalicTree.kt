package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import me.proxer.app.ui.view.bbcode2.BBUtils

/**
 * @author Ruben Gees
 */
class ItalicTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("i", ignoreCase = true)

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViewsWithoutMerging(context)

        return BBUtils.applyToTextViews(childViews) { view ->
            view.text = SpannableStringBuilder(view.text).apply {
                setSpan(StyleSpan(Typeface.ITALIC), 0, view.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
