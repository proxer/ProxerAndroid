package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.support.v4.text.util.LinkifyCompat
import android.support.v7.widget.AppCompatTextView
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.util.Linkify

/**
 * @author Ruben Gees
 */
class TextLeaf(
        private val text: String,
        parent: BBTree?,
        children: MutableList<BBTree> = mutableListOf()
) : BBTree(parent, children) {

    override fun endsWith(code: String) = false

    override fun makeViews(context: Context) = listOf(AppCompatTextView(context).also {
        it.movementMethod = LinkMovementMethod.getInstance()
        it.text = SpannableStringBuilder(text)

        LinkifyCompat.addLinks(it, Linkify.WEB_URLS)
    })
}
