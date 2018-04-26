package me.proxer.app.ui.view.bbcode.prototype

import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.SuperscriptSpan
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object SuperscriptPrototype : TextMutatorPrototype {

    override val startRegex = Regex(" *sup( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *sup *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>) = text.apply {
        setSpan(SuperscriptSpan(), 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
    }
}
