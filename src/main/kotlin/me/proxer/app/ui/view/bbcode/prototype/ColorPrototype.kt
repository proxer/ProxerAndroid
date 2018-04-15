package me.proxer.app.ui.view.bbcode.prototype

import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.compat.HtmlCompat

/**
 * @author Ruben Gees
 */
object ColorPrototype : TextMutatorPrototype {

    private val ATTRIBUTE_REGEX = Regex("color *= *(.+?)( |$)", REGEX_OPTIONS)
    private const val COLOR_ARGUMENT = "color"

    override val startRegex = Regex(" *color *= *\"?.*?\"?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *color *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val value = BBUtils.cutAttribute(code, ATTRIBUTE_REGEX) ?: ""

        val color = HtmlCompat.fromHtml("<font color='$value'>dummy</font>")
            .let { it.getSpans(0, it.length, ForegroundColorSpan::class.java) }
            .firstOrNull()?.foregroundColor

        return when (color) {
            null -> BBTree(this, parent, args = mutableMapOf())
            else -> BBTree(this, parent, args = mutableMapOf(COLOR_ARGUMENT to color))
        }
    }

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        val color = args[COLOR_ARGUMENT] as Int?

        return when (color) {null -> text
            else -> text.apply {
                setSpan(ForegroundColorSpan(color), 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
