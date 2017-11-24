package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StrikethroughSpan
import me.proxer.app.ui.view.bbcode2.BBUtils

/**
 * @author Ruben Gees
 */
class StrikethroughTree(parent: BBTree?, children: MutableList<BBTree>) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("s", ignoreCase = true)

    override fun makeViews(context: Context) = BBUtils.applyToTextViews(super.makeViewsWithoutMerging(context)) { view ->
        view.text = SpannableStringBuilder(view.text).apply {
            setSpan(StrikethroughSpan(), 0, view.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
