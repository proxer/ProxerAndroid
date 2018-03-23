package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.support.v4.widget.TextViewCompat
import android.text.SpannableStringBuilder
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import me.proxer.app.R
import me.proxer.app.ui.view.GifAwareTextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.saket.bettermovementmethod.BetterLinkMovementMethod

/**
 * @author Ruben Gees
 */
object TextPrototype : BBPrototype {

    private const val TEXT_ARGUMENT = "text"

    override val startRegex = Regex("x^")
    override val endRegex = Regex("x^")

    override fun construct(code: String, parent: BBTree): BBTree {
        return BBTree(this, parent, args = mutableMapOf(TEXT_ARGUMENT to code))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val text = args[TEXT_ARGUMENT] as CharSequence

        return listOf(makeView(context, text))
    }

    fun makeView(context: Context, text: CharSequence): TextView {
        return GifAwareTextView(context).also {
            it.text = SpannableStringBuilder(text)

            TextViewCompat.setTextAppearance(it, R.style.TextAppearance_AppCompat_Small)
            BetterLinkMovementMethod.linkify(Linkify.WEB_URLS, it)
        }
    }

    fun getText(args: Map<String, Any?>) = args[TEXT_ARGUMENT] as CharSequence

    fun updateText(newText: CharSequence, args: MutableMap<String, Any?>) {
        args[TEXT_ARGUMENT] = newText
    }
}
