package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder

/**
 * @author Ruben Gees
 */
interface TextMutatorPrototype : BBPrototype {

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(parent, args) }

        return applyToViews(childViews) { view: TextView ->
            view.text = mutate(view.text.toSpannableStringBuilder(), args)
        }
    }

    fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder
}
