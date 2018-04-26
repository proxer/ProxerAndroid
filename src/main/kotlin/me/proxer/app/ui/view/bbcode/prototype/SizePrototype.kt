package me.proxer.app.ui.view.bbcode.prototype

import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object SizePrototype : TextMutatorPrototype {

    private val ATTRIBUTE_REGEX = Regex("size *= *(.+?)( |$)", REGEX_OPTIONS)

    private const val SIZE_ARGUMENT = "size"
    private const val SIZE_TYPE_ARGUMENT = "sizeType"

    override val startRegex = Regex(" *size *= *\"?([1-6]|\\d+px)\"?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *size *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val value = BBUtils.cutAttribute(code, ATTRIBUTE_REGEX)

        return if (value?.endsWith("px") == true) {
            BBTree(this, parent, args = mutableMapOf(
                SIZE_ARGUMENT to value.substringBefore("px").toFloat(),
                SIZE_TYPE_ARGUMENT to SizeType.ABSOLUTE
            ))
        } else {
            val size = when (value) {
                "1" -> 0.4f
                "2" -> 0.7f
                "3" -> 0.85f
                "4" -> 1.0f
                "5" -> 1.5f
                "6" -> 2.0f
                else -> throw IllegalArgumentException("Unknown size: $value")
            }

            BBTree(this, parent, args = mutableMapOf(
                SIZE_ARGUMENT to size,
                SIZE_TYPE_ARGUMENT to SizeType.RELATIVE
            ))
        }
    }

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        val sizeType = args[SIZE_TYPE_ARGUMENT] as SizeType
        val size = args[SIZE_ARGUMENT] as Float

        return text.apply {
            val span: CharacterStyle = when (sizeType) {
                SizeType.RELATIVE -> RelativeSizeSpan(size)
                SizeType.ABSOLUTE -> AbsoluteSizeSpan(size.toInt(), true)
            }

            setSpan(span, 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    private enum class SizeType { RELATIVE, ABSOLUTE }
}
