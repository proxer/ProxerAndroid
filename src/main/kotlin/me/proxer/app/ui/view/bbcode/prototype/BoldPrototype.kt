package me.proxer.app.ui.view.bbcode.prototype

import android.graphics.Typeface.BOLD
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object BoldPrototype : TextMutatorPrototype {

    override val startRegex = Regex(" *b( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *b *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        return text.apply {
            setSpan(StyleSpan(BOLD), 0, text.length, SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
