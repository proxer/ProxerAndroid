package me.proxer.app.ui.view.bbcode.prototype

import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.UrlClickableSpan
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.Utils
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object UrlPrototype : TextMutatorPrototype, AutoClosingPrototype {

    private val ATTRIBUTE_REGEX = Regex("url *= *(.+?)( |$)", REGEX_OPTIONS)
    private const val URL_ARGUMENT = "url"

    private val INVALID_URL = HttpUrl.parse("https://proxer.me/404")
            ?: throw IllegalArgumentException("Could not parse url")

    override val startRegex = Regex(" *url *= *.+?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *url *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val url = BBUtils.cutAttribute(code, ATTRIBUTE_REGEX)?.trim() ?: ""
        val parsedUrl = Utils.safelyParseAndFixUrl(url) ?: INVALID_URL

        return BBTree(this, parent, args = mutableMapOf(URL_ARGUMENT to parsedUrl))
    }

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        val url = args[URL_ARGUMENT] as HttpUrl

        return text.apply {
            setSpan(UrlClickableSpan(url), 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
