package me.proxer.app.ui.view.bbcode.prototype

import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import org.jetbrains.anko.dip

object QuotePrototype : AutoClosingPrototype {

    private val QUOTE_ATTRIBUTE_REGEX = Regex("quote *= *(.+?)( |$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *quote( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *quote *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val quote = BBUtils.cutAttribute(code, QUOTE_ATTRIBUTE_REGEX)

        if (quote != null) {
            val quoteText = globalContext.getString(R.string.view_bbcode_quote, quote.trim())
            val quoteTree = BoldPrototype.construct("", parent)

            quoteTree.children.add(TextPrototype.construct(quoteText, quoteTree))
            parent.children.add(quoteTree)
        }

        return BBTree(this, parent)
    }

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = super.makeViews(parent, children, args)

        val layout = when (childViews.size) {
            0 -> null
            1 -> FrameLayout(parent.context)
            else -> LinearLayout(parent.context).apply { orientation = VERTICAL }
        }

        layout?.apply {
            val fourDip = parent.dip(4)

            layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)

            setPadding(fourDip, fourDip, fourDip, fourDip)
            setBackgroundColor(ContextCompat.getColor(parent.context, R.color.selected))

            childViews.forEach { addView(it) }
        }

        return if (layout != null) listOf(layout) else emptyList()
    }
}
