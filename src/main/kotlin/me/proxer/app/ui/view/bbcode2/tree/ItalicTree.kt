package me.proxer.app.ui.view.bbcode2.tree

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import me.proxer.app.ui.view.bbcode2.BBUtils

/**
 * @author Ruben Gees
 */
class ItalicTree(parent: BBTree?, children: MutableList<BBTree>) : BBTree(parent, children) {

    override fun endsWith(code: String) = code.startsWith("i", ignoreCase = true)

    override fun makeViews(context: Context) = BBUtils.applyToTextViews(super.makeViews(context)) { view ->
        view.text = SpannableString(view.text).apply {
            setSpan(StyleSpan(Typeface.ITALIC), 0, view.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
