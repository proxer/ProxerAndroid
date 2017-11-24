package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.UnderlineSpan
import me.proxer.app.ui.view.bbcode2.BBUtils

/**
 * @author Ruben Gees
 */
class UnderlineTree(parent: BBTree?, children: MutableList<BBTree>) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("u", ignoreCase = true)

    override fun makeViews(context: Context) = BBUtils.applyToTextViews(super.makeViewsWithoutMerging(context)) { view ->
        view.text = SpannableStringBuilder(view.text).apply {
            setSpan(UnderlineSpan(), 0, view.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
