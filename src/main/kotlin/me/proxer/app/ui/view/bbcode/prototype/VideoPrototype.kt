package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.linkifyUrl
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.extension.toPrefixedUrlOrNull

/**
 * @author Ruben Gees
 */
object VideoPrototype : TextMutatorPrototype, AutoClosingPrototype {

    private const val TYPE_ARGUMENT = "type"
    private const val TYPE_YOUTUBE = "youtube"
    private const val TYPE_YOUTUBE_URL = "https://youtu.be/"

    private val attributeRegex = Regex("type *= *(.+?)( |$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *video( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *video *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val type = BBUtils.cutAttribute(code, attributeRegex)

        return BBTree(this, parent, args = BBArgs(custom = arrayOf(TYPE_ARGUMENT to type)))
    }

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val type = args[TYPE_ARGUMENT] as String?
        val urlOrId = text.trim()

        val url = when {
            type?.equals(TYPE_YOUTUBE, ignoreCase = true) == true -> "$TYPE_YOUTUBE_URL$urlOrId"
            else -> urlOrId.toString()
        }.toPrefixedUrlOrNull()

        return when (url) {
            null -> text
            else ->
                text.toSpannableStringBuilder()
                    .replace(0, text.length, args.safeResources.getString(R.string.view_bbcode_video_link))
                    .linkifyUrl(url)
        }
    }
}
