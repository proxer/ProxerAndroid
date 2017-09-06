package me.proxer.app.ui.view.bbcode

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import me.proxer.app.ui.view.bbcode.BBElement.BBSpoilerElement
import me.proxer.app.ui.view.bbcode.BBElement.BBTextElement
import me.proxer.app.ui.view.bbcode.BBTokenizer.BBToken
import me.proxer.app.ui.view.bbcode.BBTokenizer.BBTokenType.*
import java.util.*

/**
 * @author Ruben Gees
 */
internal object BBProcessor {

    internal fun process(tree: BBToken) = trimElements(mergeFittingElements(processWithStyle(tree, BBStyle())))

    private fun processWithStyle(tree: BBToken, style: BBStyle): List<BBElement> = when (tree.type) {
        TOKEN_TEXT -> listOf(handleTextToken(tree, style))
        TOKEN_SPOILER -> handleSpoilerToken(tree, style)
        else -> tree.children.flatMap {
            processWithStyle(it, when (tree.type) {
                TOKEN_ROOT -> style
                TOKEN_SIZE -> style.copy(size = tree.attribute as Float)
                TOKEN_BOLD -> style.copy(isBold = true)
                TOKEN_ITALIC -> style.copy(isItalic = true)
                TOKEN_UNDERLINE -> style.copy(isUnderlined = true)
                TOKEN_LEFT -> style.copy(gravity = Gravity.START)
                TOKEN_CENTER -> style.copy(gravity = Gravity.CENTER_HORIZONTAL)
                TOKEN_RIGHT -> style.copy(gravity = Gravity.END)
                TOKEN_COLOR -> style.copy(color = tree.attribute as String)
                else -> throw IllegalArgumentException("Invalid token: ${tree.type}")
            })
        }
    }

    private fun handleSpoilerToken(tree: BBToken, style: BBStyle) = tree.children
            .flatMap { processWithStyle(it, style.copy()) }
            .let {
                when (it.isEmpty()) {
                    true -> it
                    false -> it.plus(BBSpoilerElement(it))
                }
            }

    private fun handleTextToken(tree: BBToken, style: BBStyle): BBElement {
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

        return BBTextElement(styledText, style.gravity)
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
                is BBTextElement -> when (nextElement) {
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
            is BBTextElement -> element.text.delete(0, element.text.indexOfFirst { !it.isWhitespace() })
            is BBSpoilerElement -> if (element.children.isNotEmpty()) {
                trimFirstTextElement(element.children.first())
            }
        }
    }

    private fun trimLastTextElement(element: BBElement) {
        when (element) {
            is BBTextElement -> element.text.delete(element.text.indexOfLast { !it.isWhitespace() } + 1,
                    element.text.length)
            is BBSpoilerElement -> if (element.children.isNotEmpty()) {
                trimFirstTextElement(element.children.last())
            }
        }
    }

    private data class BBStyle constructor(val gravity: Int = Gravity.START, val size: Float = 1.0f,
                                           val color: String? = null, val isBold: Boolean = false,
                                           val isItalic: Boolean = false, val isUnderlined: Boolean = false)
}
