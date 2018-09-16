package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import android.text.style.UnderlineSpan
import androidx.core.text.set
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object UnderlinePrototype : TextMutatorPrototype {

    override val startRegex = Regex(" *u( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *u *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: BBArgs) = text.apply {
        this[0..length] = UnderlineSpan()
    }
}
