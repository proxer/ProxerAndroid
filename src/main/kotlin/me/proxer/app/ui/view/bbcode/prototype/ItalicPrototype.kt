package me.proxer.app.ui.view.bbcode.prototype

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.core.text.set
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object ItalicPrototype : TextMutatorPrototype {

    override val startRegex = Regex(" *i( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *i *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: BBArgs) = text.apply {
        this[0..length] = StyleSpan(Typeface.ITALIC)
    }
}
