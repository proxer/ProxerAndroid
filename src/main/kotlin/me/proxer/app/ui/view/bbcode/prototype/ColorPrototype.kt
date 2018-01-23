package me.proxer.app.ui.view.bbcode.prototype

import android.graphics.Color
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object ColorPrototype : TextMutatorPrototype {

    private val ATTRIBUTE_REGEX = Regex("color *= *(.+?)( |$)", REGEX_OPTIONS)
    private const val COLOR_ARGUMENT = "color"

    private val availableColors = arrayOf(
            "black" to Color.parseColor("#000000"),
            "orange" to Color.parseColor("#f8a523"),
            "red" to Color.parseColor("#f50022"),
            "blue" to Color.parseColor("#3000fd"),
            "purple" to Color.parseColor("#7c007f"),
            "green" to Color.parseColor("#238107"),
            "white" to Color.parseColor("#ffffff"),
            "gray" to Color.parseColor("#7f7f7f")
    )

    private val availableColorsForRegex = availableColors.joinToString("|") { it.first }
    private val colorsForRegex = "(#[A-Fa-f0-9]{6}|#[A-Fa-f0-9]{8}|$availableColorsForRegex)"

    override val startRegex = Regex(" *color *= *\"?$colorsForRegex\"?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *color *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val value = BBUtils.cutAttribute(code, ATTRIBUTE_REGEX) ?: ""
        val color = when (value.startsWith("#")) {
            true -> Color.parseColor(value)
            false -> availableColors.find { it.first.equals(value, ignoreCase = true) }?.second
                    ?: throw IllegalArgumentException("Unknown color: $value")
        }

        return BBTree(this, parent, args = mutableMapOf(COLOR_ARGUMENT to color))
    }

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        val color = args[COLOR_ARGUMENT] as Int

        return text.apply {
            setSpan(ForegroundColorSpan(color), 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
