package me.proxer.app.ui.view.bbcode.prototype

import android.graphics.Typeface.BOLD
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.core.text.set
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object BoldPrototype : TextMutatorPrototype {

    override val startRegex = Regex(" *b( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *b *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: BBArgs) = text.apply {
        this[0..length] = StyleSpan(BOLD)
    }
}
