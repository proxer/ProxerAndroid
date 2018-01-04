package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.graphics.Typeface.BOLD
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder

/**
 * @author Ruben Gees
 */
object BoldPrototype : BBPrototype {

    override val startRegex = Regex(" *b( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *b *", REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }

        return applyToViews(childViews) { view: TextView ->
            view.text = view.text.toSpannableStringBuilder().apply {
                setSpan(StyleSpan(BOLD), 0, view.length(), SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
