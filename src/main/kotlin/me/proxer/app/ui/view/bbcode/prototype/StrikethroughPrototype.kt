package me.proxer.app.ui.view.bbcode.prototype

import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.StrikethroughSpan
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object StrikethroughPrototype : TextMutatorPrototype {

    override val startRegex = Regex(" *(s|strike)( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *(s|strike) *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        return text.apply {
            setSpan(StrikethroughSpan(), 0, text.length, SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
