package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.linkifyUrl
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.extension.toPrefixedUrlOrNull

/**
 * @author Ruben Gees
 */
object FacebookPrototype : TextMutatorPrototype, AutoClosingPrototype {

    override val startRegex = Regex(" *facebook_link( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *facebook_link *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val url = text.trim().toString()

        return when (val parsedUrl = url.toPrefixedUrlOrNull()) {
            null -> text
            else ->
                text.toSpannableStringBuilder()
                    .replace(0, text.length, args.safeResources.getString(R.string.view_bbcode_facebook_link))
                    .linkifyUrl(parsedUrl)
        }
    }
}
