package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder

/**
 * @author Ruben Gees
 */
interface TextMutatorPrototype : BBPrototype {

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }

        return applyToViews(childViews) { view: TextView ->
            view.text = mutate(view.text.toSpannableStringBuilder(), args)
        }
    }

    fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder
}
