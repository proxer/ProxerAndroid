package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import me.proxer.app.ui.view.bbcode2.BBUtils

/**
 * @author Ruben Gees
 */
class BoldTree(parent: BBTree?, children: MutableList<BBTree>) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("b", ignoreCase = true)

    override fun makeViews(context: Context) = BBUtils.applyToTextViews(super.makeViewsWithoutMerging(context)) { view ->
        view.text = SpannableStringBuilder(view.text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, view.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
