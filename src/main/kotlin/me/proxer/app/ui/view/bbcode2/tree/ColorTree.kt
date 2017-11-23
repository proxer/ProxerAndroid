package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.support.annotation.ColorInt
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import me.proxer.app.ui.view.bbcode2.BBUtils.applyToTextViews

/**
 * @author Ruben Gees
 */
class ColorTree(parent: BBTree?, children: MutableList<BBTree>, @ColorInt val color: Int) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("color", ignoreCase = true)

    override fun makeViews(context: Context) = applyToTextViews(super.makeViewsWithoutMerging(context)) { view ->
        view.text = SpannableStringBuilder(view.text).apply {
            setSpan(ForegroundColorSpan(color), 0, view.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
