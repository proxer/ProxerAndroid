package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.text.Layout.Alignment.ALIGN_OPPOSITE
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.AlignmentSpan
import android.view.Gravity.END
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder

/**
 * @author Ruben Gees
 */
object RightPrototype : BBPrototype {

    override val startRegex = Regex(" *right( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *right *", REGEX_OPTIONS)

    @Suppress("OptionalWhenBraces")
    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }

        return applyToViews(childViews) { view: View ->
            when (view) {
                is TextView -> {
                    view.text = view.text.toSpannableStringBuilder().apply {
                        setSpan(AlignmentSpan.Standard(ALIGN_OPPOSITE), 0, view.length(), SPAN_INCLUSIVE_EXCLUSIVE)
                    }
                }
                is LinearLayout -> view.gravity = END
                else -> (view.layoutParams as? LayoutParams)?.gravity = END
            }
        }
    }
}
