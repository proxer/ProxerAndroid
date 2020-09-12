package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import androidx.core.text.set
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object SizePrototype : TextMutatorPrototype {

    private const val SIZE_ARGUMENT = "size"
    private const val SIZE_TYPE_ARGUMENT = "sizeType"

    private val attributeRegex = Regex("size *= *(.+?)( |$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *size *= *\"?([1-6]|\\d+px)\"?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *size *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val value = BBUtils.cutAttribute(code, attributeRegex)

        return if (value?.endsWith("px") == true) {
            BBTree(
                this,
                parent,
                args = BBArgs(
                    custom = arrayOf(
                        SIZE_ARGUMENT to value.substringBeforeLast("px").toFloat(),
                        SIZE_TYPE_ARGUMENT to SizeType.ABSOLUTE
                    )
                )
            )
        } else {
            val size = when (value) {
                "1" -> 0.7f
                "2" -> 0.85f
                "3" -> 1.0f
                "4" -> 1.5f
                "5" -> 2.0f
                "6" -> 3.0f
                else -> error("Unknown size: $value")
            }

            BBTree(
                this,
                parent,
                args = BBArgs(
                    custom = arrayOf(
                        SIZE_ARGUMENT to size,
                        SIZE_TYPE_ARGUMENT to SizeType.RELATIVE
                    )
                )
            )
        }
    }

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val sizeType = args[SIZE_TYPE_ARGUMENT] as SizeType
        val size = args[SIZE_ARGUMENT] as Float

        return text.apply {
            val span: CharacterStyle = when (sizeType) {
                SizeType.RELATIVE -> RelativeSizeSpan(size)
                SizeType.ABSOLUTE -> AbsoluteSizeSpan(size.toInt(), true)
            }

            this[0..length] = span
        }
    }

    private enum class SizeType { RELATIVE, ABSOLUTE }
}
