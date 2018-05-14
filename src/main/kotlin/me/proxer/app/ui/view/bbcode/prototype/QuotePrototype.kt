package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
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

    override fun makeViews(context: Context, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = super.makeViews(context, children, args)

        val layout = when (childViews.size) {
            0 -> null
            1 -> FrameLayout(context)
            else -> LinearLayout(context)
        }

        layout?.apply {
            val fourDip = context.dip(4)

            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            setPadding(fourDip, fourDip, fourDip, fourDip)
            setBackgroundColor(ContextCompat.getColor(context, R.color.selected))

            childViews.forEach { addView(it) }
        }

        return if (layout != null) listOf(layout) else emptyList()
    }
}
