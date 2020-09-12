package me.proxer.app.ui.view.bbcode.prototype

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.resolveColor

object QuotePrototype : AutoClosingPrototype {

    private const val QUOTE_ARGUMENT = "quote"

    private val quoteAttributeRegex = Regex("quote *= *(.+?)( |$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *quote( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *quote *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val quote = BBUtils.cutAttribute(code, quoteAttributeRegex)

        return BBTree(this, parent, args = BBArgs(custom = arrayOf(QUOTE_ARGUMENT to quote)))
    }

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = super.makeViews(parent, children, args)
        val quote = args[QUOTE_ARGUMENT] as String?
        val result = mutableListOf<View>()

        val layout = when (childViews.size) {
            0 -> null
            1 -> FrameLayout(parent.context)
            else -> LinearLayout(parent.context).apply { orientation = VERTICAL }
        }

        layout?.apply {
            val fourDip = parent.dip(4)

            layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)

            setPadding(fourDip, fourDip, fourDip, fourDip)
            setBackgroundColor(parent.context.resolveColor(R.attr.colorSelectedSurface))

            childViews.forEach { addView(it) }
        }

        if (quote != null) {
            val quoteText = args.safeResources.getString(R.string.view_bbcode_quote, quote.trim())
            val boldQuoteText = BoldPrototype.mutate(quoteText.linkify().toSpannableStringBuilder(), args)

            result += TextPrototype.makeView(parent, args + BBArgs(text = boldQuoteText))
        }

        if (layout != null) {
            result += layout
        }

        return result
    }
}
