package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import android.text.style.StrikethroughSpan
import androidx.core.text.set
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object StrikethroughPrototype : TextMutatorPrototype {

    override val startRegex = Regex(" *(s|strike)( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *(s|strike) *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: BBArgs) = text.apply {
        this[0..length] = StrikethroughSpan()
    }
}
