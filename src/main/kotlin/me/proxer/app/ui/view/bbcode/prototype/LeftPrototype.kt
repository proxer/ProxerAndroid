package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.text.Layout.Alignment.ALIGN_NORMAL
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.AlignmentSpan
import android.view.Gravity.START
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder

/**
 * @author Ruben Gees
 */
object LeftPrototype : BBPrototype {

    override val startRegex = Regex(" *left( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *left *", REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }

        return applyToViews(childViews) { view: View ->
            if (view is TextView) {
                view.text = view.text.toSpannableStringBuilder().apply {
                    setSpan(AlignmentSpan.Standard(ALIGN_NORMAL), 0, view.length(), SPAN_INCLUSIVE_EXCLUSIVE)
                }
            } else if (view is ImageView) {
                (view.layoutParams as? LayoutParams)?.gravity = START
            }
        }
    }
}
