package com.proxerme.app.view.bbcode

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object BBProcessor {

    fun process(tree: BBTokenizer.BBToken): List<BBElement> {
        return trimElements(mergeFittingElements(processWithStyle(tree, BBStyle())))
    }

    /**
     * (•_•)
     * ( •_•)>⌐■-■
     * (⌐■_■)
     */
    private fun processWithStyle(tree: BBTokenizer.BBToken, style: BBStyle): List<BBElement> {
        val result = ArrayList<BBElement>()

        when (tree.type) {
            BBTokenizer.TOKEN_ROOT -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style))
                }
            }

            BBTokenizer.TOKEN_TEXT -> {
                val text = tree.attribute as String
                val styledText = SpannableString(text)

                styledText.setSpan(RelativeSizeSpan(style.size), 0, text.length,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

                if (style.isBold) {
                    if (style.isItalic) {
                        styledText.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, text.length,
                                Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                    } else {
                        styledText.setSpan(StyleSpan(Typeface.BOLD), 0, text.length,
                                Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                    }
                } else if (style.isItalic) {
                    styledText.setSpan(StyleSpan(Typeface.ITALIC), 0, text.length,
                            Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }

                if (style.isUnderlined) {
                    styledText.setSpan(UnderlineSpan(), 0, text.length,
                            Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }

                if (style.color != null) {
                    styledText.setSpan(ForegroundColorSpan(Color.parseColor(style.color)), 0,
                            text.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }

                result.add(BBTextElement(styledText, style.gravity))
            }

            BBTokenizer.TOKEN_SIZE -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.clone().apply {
                        size = tree.attribute as Float
                    }))
                }
            }

            BBTokenizer.TOKEN_BOLD -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.clone().apply {
                        isBold = true
                    }))
                }
            }

            BBTokenizer.TOKEN_ITALIC -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.clone().apply {
                        isItalic = true
                    }))
                }
            }

            BBTokenizer.TOKEN_UNDERLINE -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.clone().apply {
                        isUnderlined = true
                    }))
                }
            }

            BBTokenizer.TOKEN_LEFT -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.clone().apply {
                        gravity = Gravity.START
                    }))
                }
            }

            BBTokenizer.TOKEN_CENTER -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.clone().apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }))
                }
            }

            BBTokenizer.TOKEN_RIGHT -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.clone().apply {
                        gravity = Gravity.END
                    }))
                }
            }

            BBTokenizer.TOKEN_COLOR -> {
                tree.children.forEach {
                    result.addAll(processWithStyle(it, style.clone().apply {
                        color = tree.attribute as String
                    }))
                }
            }

            BBTokenizer.TOKEN_SPOILER -> {
                if (tree.children.isNotEmpty()) {
                    val spoilerChildren = LinkedList<BBElement>()

                    for (child in tree.children) {
                        spoilerChildren.addAll(processWithStyle(child, style.clone()))
                    }

                    result.add(BBSpoilerElement(spoilerChildren))
                }
            }
        }

        return result
    }

    private fun mergeFittingElements(list: List<BBElement>): List<BBElement> {
        if (list.isEmpty()) {
            return emptyList()
        }

        val result = ArrayList<BBElement>()
        var index = 0
        var nextIndex = 1

        result.add(list.first())

        while (nextIndex < list.size) {
            val currentElement = result[index]
            val nextElement = list[nextIndex]

            if (currentElement is BBSpoilerElement) {
                result.add(nextElement)

                index++
            } else {
                currentElement as BBTextElement

                if (nextElement is BBTextElement) {
                    if (currentElement.gravity == nextElement.gravity) {
                        result[index] = BBTextElement(TextUtils.concat(currentElement.text,
                                nextElement.text), currentElement.gravity)
                    } else {
                        result.add(nextElement)

                        index++
                    }
                } else {
                    nextElement as BBSpoilerElement

                    result.add(BBSpoilerElement(mergeFittingElements(nextElement.children)))

                    index++
                }
            }

            nextIndex++
        }

        return result
    }

    private fun trimElements(list: List<BBElement>): List<BBElement> {
        if (list.isEmpty()) {
            return emptyList()
        }

        val result = ArrayList<BBElement>()
        var trimStartOfNextItem = false

        for (i in 0 until list.size) {
            var currentElement = list[i]
            val nextElement = list.getOrNull(i + 1)

            if (currentElement is BBSpoilerElement) {
                result.add(BBSpoilerElement(trimElements(currentElement.children)))

                trimStartOfNextItem = false
            } else {
                currentElement as BBTextElement

                if (nextElement is BBSpoilerElement) {
                    result.add(currentElement.trimEndIfPossible())

                    trimStartOfNextItem = false
                } else if (nextElement is BBTextElement) {
                    if (trimStartOfNextItem) {
                        currentElement = currentElement.trimStartIfPossible()

                        trimStartOfNextItem = false
                    }

                    if (currentElement.canTrimEnd()) {
                        result.add(currentElement.trimEnd())
                    } else if (nextElement.canTrimStart()) {
                        result.add(currentElement)
                    } else {
                        result.add(currentElement)

                        trimStartOfNextItem = true
                    }
                } else {
                    result.add(currentElement)
                }
            }
        }

        val first = result.first()
        val last = result.last()

        if (first is BBTextElement) {
            result[0] = BBTextElement(first.text.trimStart(), first.gravity)
        }

        if (last is BBTextElement) {
            result[result.lastIndex] = BBTextElement(last.text.trimEnd(), last.gravity)
        }

        return result
    }

    abstract class BBElement

    class BBTextElement(val text: CharSequence, val gravity: Int) : BBElement() {

        companion object {
            private const val NEWLINE_PATTERN = "\n"
        }

        fun canTrimStart() = text.startsWith(NEWLINE_PATTERN)
        fun canTrimEnd() = text.endsWith(NEWLINE_PATTERN)

        fun trimStart() = BBTextElement(text.substring(NEWLINE_PATTERN.length, text.length),
                gravity)

        fun trimStartIfPossible() = if (canTrimStart()) trimStart() else this

        fun trimEnd() = BBTextElement(text.substring(0, text.length - NEWLINE_PATTERN.length),
                gravity)

        fun trimEndIfPossible() = if (canTrimEnd()) trimEnd() else this
    }

    class BBSpoilerElement(val children: List<BBElement>) : BBElement()

    private class BBStyle {
        var gravity = Gravity.START
        var size = 1.0f
        var color: String? = null
        var isBold = false
        var isItalic = false
        var isUnderlined = false

        constructor()

        private constructor(gravity: Int, size: Float, color: String?, isBold: Boolean,
                            isItalic: Boolean, isUnderlined: Boolean) {
            this.gravity = gravity
            this.size = size
            this.color = color
            this.isBold = isBold
            this.isItalic = isItalic
            this.isUnderlined = isUnderlined
        }

        fun clone(): BBStyle {
            return BBStyle(gravity, size, color, isBold, isItalic, isUnderlined)
        }
    }

}