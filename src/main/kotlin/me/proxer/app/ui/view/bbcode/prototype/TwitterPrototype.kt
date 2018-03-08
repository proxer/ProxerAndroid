package me.proxer.app.ui.view.bbcode.prototype

import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.UrlClickableSpan
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object TwitterPrototype : TextMutatorPrototype, AutoClosingPrototype {

    override val startRegex = Regex(" *tweet( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *tweet *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        val id = text.trim()
        val url = HttpUrl.parse("https://twitter.com/i/web/status/$id")

        return when (url) {
            null -> text
            else -> globalContext.getString(R.string.view_bbcode_twitter_link).toSpannableStringBuilder().apply {
                setSpan(UrlClickableSpan(url), 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
