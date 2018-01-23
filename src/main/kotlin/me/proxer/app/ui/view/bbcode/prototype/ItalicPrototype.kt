package me.proxer.app.ui.view.bbcode.prototype

import android.graphics.Typeface.ITALIC
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object ItalicPrototype : TextMutatorPrototype {

    override val startRegex = Regex(" *i( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *i *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        return text.apply {
            setSpan(StyleSpan(ITALIC), 0, text.length, SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
