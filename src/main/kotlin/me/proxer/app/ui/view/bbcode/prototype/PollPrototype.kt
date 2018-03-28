package me.proxer.app.ui.view.bbcode.prototype

import android.text.Spannable
import android.text.SpannableStringBuilder
import me.proxer.app.MainApplication
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.UrlClickableSpan
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils

object PollPrototype : TextMutatorPrototype, AutoClosingPrototype {

    override val startRegex = Regex(" *poll( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *poll *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        val id = text.trim()
        val url = ProxerUrls.webBase().newBuilder()
            .addPathSegments("poll/$id")
            .addQueryParameter("device", ProxerUtils.getApiEnumName(Device.MOBILE))
            .build()

        return when (url) {
            null -> text
            else -> MainApplication.globalContext.getString(R.string.view_bbcode_poll_link).toSpannableStringBuilder().apply {
                setSpan(UrlClickableSpan(url), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
