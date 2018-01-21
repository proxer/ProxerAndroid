package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.graphics.Color
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder

/**
 * @author Ruben Gees
 */
object ColorPrototype : BBPrototype {

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

        return BBTree(this, parent, args = mapOf(COLOR_ARGUMENT to color))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }
        val color = args[COLOR_ARGUMENT] as Int

        return applyToViews(childViews) { view: TextView ->
            view.text = view.text.toSpannableStringBuilder().apply {
                setSpan(ForegroundColorSpan(color), 0, view.length(), SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
