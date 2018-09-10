package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.linkifyUrl
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object TwitterPrototype : TextMutatorPrototype, AutoClosingPrototype {

    override val startRegex = Regex(" *tweet( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *tweet *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val id = text.trim()
        val url = HttpUrl.parse("https://twitter.com/i/web/status/$id")

        return when (url) {
            null -> text
            else -> text.toSpannableStringBuilder()
                .replace(0, text.length, args.safeResources.getString(R.string.view_bbcode_twitter_link))
                .linkifyUrl(url)
        }
    }
}
