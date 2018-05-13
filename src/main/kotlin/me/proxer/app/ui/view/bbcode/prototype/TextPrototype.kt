package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.support.v4.widget.TextViewCompat
import android.view.View
import android.widget.TextView
import me.proxer.app.R
import me.proxer.app.ui.view.GifAwareTextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.extension.linkify
import me.saket.bettermovementmethod.BetterLinkMovementMethod

/**
 * @author Ruben Gees
 */
object TextPrototype : BBPrototype {

    private const val TEXT_ARGUMENT = "text"

    override val startRegex = Regex("x^")
    override val endRegex = Regex("x^")

    override fun construct(code: String, parent: BBTree): BBTree {
        return BBTree(this, parent, args = mutableMapOf(TEXT_ARGUMENT to code.toSpannableStringBuilder().linkify()))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val text = args[TEXT_ARGUMENT] as CharSequence

        return listOf(makeView(context, text))
    }

    fun makeView(context: Context, text: CharSequence): TextView {
        return applyOnView(GifAwareTextView(context), text)
    }

    fun applyOnView(view: GifAwareTextView, args: Map<String, Any?>): GifAwareTextView {
        val text = args[TEXT_ARGUMENT] as CharSequence

        return applyOnView(view, text)
    }

    fun getText(args: Map<String, Any?>) = args[TEXT_ARGUMENT] as CharSequence

    fun updateText(newText: CharSequence, args: MutableMap<String, Any?>) {
        args[TEXT_ARGUMENT] = newText
    }

    private fun applyOnView(view: GifAwareTextView, text: CharSequence): GifAwareTextView {
        view.text = text
        view.movementMethod = BetterLinkMovementMethod.newInstance()

        TextViewCompat.setTextAppearance(view, R.style.TextAppearance_AppCompat_Small)

        return view
    }
}
