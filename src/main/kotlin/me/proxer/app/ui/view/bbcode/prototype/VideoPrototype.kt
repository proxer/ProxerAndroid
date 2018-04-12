package me.proxer.app.ui.view.bbcode.prototype

import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.UrlClickableSpan
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.Utils

/**
 * @author Ruben Gees
 */
object VideoPrototype : TextMutatorPrototype, AutoClosingPrototype {

    private val ATTRIBUTE_REGEX = Regex("type *= *(.+?)( |$)", REGEX_OPTIONS)
    private const val TYPE_ARGUMENT = "type"
    private const val TYPE_YOUTUBE = "youtube"
    private const val TYPE_YOUTUBE_URL = "https://youtu.be/"

    override val startRegex = Regex(" *video( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *video *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val type = BBUtils.cutAttribute(code, ATTRIBUTE_REGEX)

        return BBTree(this, parent, args = mutableMapOf(TYPE_ARGUMENT to type))
    }

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        val type = args[TYPE_ARGUMENT] as String?
        val urlOrId = text.trim()

        val url = Utils.safelyParseAndFixUrl(when {
            type?.equals(TYPE_YOUTUBE, ignoreCase = true) == true -> "$TYPE_YOUTUBE_URL$urlOrId"
            else -> urlOrId.toString()
        })

        return when (url) {
            null -> text
            else -> globalContext.getString(R.string.view_bbcode_video_link).toSpannableStringBuilder().apply {
                setSpan(UrlClickableSpan(url), 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
