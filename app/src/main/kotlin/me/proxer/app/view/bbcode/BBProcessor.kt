package me.proxer.app.view.bbcode

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import me.proxer.app.view.bbcode.BBProcessor.BBElement.BBSpoilerElement
import me.proxer.app.view.bbcode.BBProcessor.BBElement.BBTextElement
import me.proxer.app.view.bbcode.BBTokenizer.BBToken
import me.proxer.app.view.bbcode.BBTokenizer.BBTokenType.*
import java.util.*

/**
 * @author Ruben Gees
 */
internal object BBProcessor {

    internal fun process(tree: BBToken): List<BBElement> {
        return trimElements(mergeFittingElements(processWithStyle(tree, BBStyle())))
    }

    /**
     * (•_•)
     * ( •_•)>⌐■-■
     * (⌐■_■)
     */
    private fun processWithStyle(tree: BBToken, style: BBStyle): List<BBElement> {
        val result = ArrayList<BBElement>()

        when (tree.type) {
            TOKEN_ROOT -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style))
                }
            }

            TOKEN_TEXT -> {
                val text = tree.attribute as String
                val styledText = SpannableStringBuilder(text)

                if (style.size != 1.0f) {
                    styledText.setSpan(RelativeSizeSpan(style.size), 0, text.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (style.isBold) {
                    if (style.isItalic) {
                        styledText.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, text.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                    } else {
                        styledText.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                } else if (style.isItalic) {
                    styledText.setSpan(StyleSpan(Typeface.ITALIC), 0, text.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (style.isUnderlined) {
                    styledText.setSpan(UnderlineSpan(), 0, text.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (style.color != null) {
                    val color = Color.parseColor(style.color)

                    styledText.setSpan(ForegroundColorSpan(color), 0, text.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                result.add(BBTextElement(styledText, style.gravity))
            }

            TOKEN_SIZE -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.copy(size = tree.attribute as Float)))
                }
            }

            TOKEN_BOLD -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.copy(isBold = true)))
                }
            }

            TOKEN_ITALIC -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.copy(isItalic = true)))
                }
            }

            TOKEN_UNDERLINE -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.copy(isUnderlined = true)))
                }
            }

            TOKEN_LEFT -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.copy(gravity = Gravity.START)))
                }
            }

            TOKEN_CENTER -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.copy(gravity = Gravity.CENTER_HORIZONTAL)))
                }
            }

            TOKEN_RIGHT -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.copy(gravity = Gravity.END)))
                }
            }

            TOKEN_COLOR -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.copy(color = tree.attribute as String)))
                }
            }

            TOKEN_SPOILER -> {
                if (tree.children.isNotEmpty()) {
                    val spoilerChildren = LinkedList<BBElement>()

                    for (child in tree.children) {
                        spoilerChildren.addAll(processWithStyle(child, style.copy()))
                    }

                    result.add(BBSpoilerElement(spoilerChildren))
                }
            }
        }

        return result
    }

    private fun mergeFittingElements(oldList: List<BBElement>): List<BBElement> {
        if (oldList.isEmpty()) {
            return emptyList()
        }

        val newList = ArrayList<BBElement>()
        var newListIndex = 0
        var oldListIndex = 1

        newList.add(oldList.first())

        while (oldListIndex < oldList.size) {
            val currentElement = newList[newListIndex]
            val nextElement = oldList[oldListIndex]

            when (currentElement) {
                is BBSpoilerElement -> {
                    when (nextElement) {
                        is BBTextElement -> {
                            newList.add(nextElement)
                        }
                        is BBSpoilerElement -> {
                            newList.add(BBSpoilerElement(mergeFittingElements(nextElement.children)))
                        }
                    }

                    newListIndex++
                }
                is BBTextElement -> {
                    when (nextElement) {
                        is BBTextElement -> {
                            if (currentElement.gravity == nextElement.gravity) {
                                currentElement.text.append(nextElement.text)
                            } else {
                                newList.add(nextElement)

                                newListIndex++
                            }
                        }
                        is BBSpoilerElement -> {
                            newList.add(BBSpoilerElement(mergeFittingElements(nextElement.children)))

                            newListIndex++
                        }
                    }
                }
            }

            oldListIndex++
        }

        return newList
    }

    private fun trimElements(list: List<BBElement>): List<BBElement> {
        if (list.isEmpty()) {
            return emptyList()
        }

        var trimStartOfNextItem = false

        for (i in 0 until list.size) {
            val currentElement = list[i]
            val nextElement = list.getOrNull(i + 1)

            when (currentElement) {
                is BBSpoilerElement -> {
                    trimElements(currentElement.children)

                    trimStartOfNextItem = false
                }
                is BBElement.BBTextElement -> {
                    if (trimStartOfNextItem) {
                        currentElement.trimStart()

                        trimStartOfNextItem = false
                    }

                    when (nextElement) {
                        is BBSpoilerElement -> {
                            currentElement.trimEnd()
                        }
                        is BBTextElement -> {
                            if (trimStartOfNextItem) {
                                currentElement.trimStart()
                            }

                            if (!currentElement.trimEnd()) {
                                trimStartOfNextItem = true
                            }
                        }
                    }
                }
            }
        }

        trimFirstTextElement(list.first())
        trimLastTextElement(list.last())

        return list
    }

    private fun trimFirstTextElement(element: BBElement) {
        when (element) {
            is BBTextElement -> {
                element.text.delete(0, element.text.indexOfFirst { !it.isWhitespace() })
            }
            is BBSpoilerElement -> {
                if (element.children.isNotEmpty()) {
                    trimFirstTextElement(element.children.first())
                }
            }
        }
    }

    private fun trimLastTextElement(element: BBElement) {
        when (element) {
            is BBTextElement -> {
                element.text.delete(element.text.indexOfLast { !it.isWhitespace() } + 1, element.text.length)
            }
            is BBSpoilerElement -> {
                if (element.children.isNotEmpty()) {
                    trimFirstTextElement(element.children.last())
                }
            }
        }
    }

    internal sealed class BBElement {
        class BBTextElement(val text: SpannableStringBuilder, val gravity: Int) : BBElement() {

            companion object {
                private const val TRIM_PATTERN = "\r\n"
                private const val ALT_TRIM_PATTERN = "\n"
            }

            fun trimStart() = when {
                text.startsWith(TRIM_PATTERN) -> {
                    text.delete(0, TRIM_PATTERN.length)

                    true
                }
                text.startsWith(ALT_TRIM_PATTERN) -> {
                    text.delete(0, ALT_TRIM_PATTERN.length)

                    true
                }
                else -> false
            }

            fun trimEnd() = when {
                text.endsWith(TRIM_PATTERN) -> {
                    text.delete(text.length - TRIM_PATTERN.length, text.length)

                    true
                }
                text.startsWith(ALT_TRIM_PATTERN) -> {
                    text.delete(text.length - ALT_TRIM_PATTERN.length, text.length)

                    true
                }
                else -> false
            }
        }

        internal class BBSpoilerElement(val children: List<BBElement>) : BBElement()
    }

    private data class BBStyle constructor(val gravity: Int = Gravity.START, val size: Float = 1.0f,
                                           val color: String? = null, val isBold: Boolean = false,
                                           val isItalic: Boolean = false, val isUnderlined: Boolean = false)
}