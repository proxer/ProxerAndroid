package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import me.proxer.app.ui.view.bbcode2.BBUtils.applyToTextViews

/**
 * @author Ruben Gees
 */
class SizeTree(parent: BBTree?, children: MutableList<BBTree>, private val relativeSize: Float) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("size", ignoreCase = true)

    override fun makeViews(context: Context) = applyToTextViews(super.makeViewsWithoutMerging(context)) { view ->
        view.text = SpannableStringBuilder(view.text).apply {
            setSpan(RelativeSizeSpan(relativeSize), 0, view.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
