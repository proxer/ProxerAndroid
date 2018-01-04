package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.support.v4.text.util.LinkifyCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatTextView
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object TextPrototype : BBPrototype {

    private const val TEXT_ARGUMENT = "text"

    override val startRegex = Regex("x^")
    override val endRegex = Regex("x^")

    override fun construct(code: String, parent: BBTree): BBTree {
        return BBTree(this, parent, args = mapOf(TEXT_ARGUMENT to code))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val text = args[TEXT_ARGUMENT] as String

        return listOf(AppCompatTextView(context).also {
            it.movementMethod = LinkMovementMethod.getInstance()
            it.text = SpannableStringBuilder(text)

            TextViewCompat.setTextAppearance(it, R.style.TextAppearance_AppCompat_Small)
            LinkifyCompat.addLinks(it, Linkify.WEB_URLS)
        })
    }
}
