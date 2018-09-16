package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import android.text.style.SuperscriptSpan
import androidx.core.text.set
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object SuperscriptPrototype : TextMutatorPrototype {

    override val startRegex = Regex(" *sup( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *sup *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: BBArgs) = text.apply {
        this[0..length] = SuperscriptSpan()
    }
}
