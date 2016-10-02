package com.proxerme.app.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.proxerme.app.R
import org.apmem.tools.layouts.FlowLayout
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class BBCodeView : FlowLayout {

    private companion object {
        private const val TOKEN_ROOT = 0
        private const val TOKEN_TEXT = 1
        private const val TOKEN_SIZE = 2
        private const val TOKEN_BOLD = 3
        private const val TOKEN_ITALIC = 4
        private const val TOKEN_UNDERLINE = 5
        private const val TOKEN_LEFT = 6
        private const val TOKEN_CENTER = 7
        private const val TOKEN_RIGHT = 8
        private const val TOKEN_COLOR = 9

        private val bbTokens = arrayOf(SizeToken(), BoldToken(), ItalicToken(), UnderlineToken(),
                LeftToken(), CenterToken(), RightToken(), ColorToken())
    }

    var bbCode: String? = null
        set(value) {
            field = value

            build()
        }

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs,
            defStyleAttr)

    private fun build() {
        removeAllViews()

        if (!bbCode.isNullOrBlank()) {
            buildViews(this, tokenize(bbCode!!), BBFlags())
        }
    }

    private fun buildViews(currentView: ViewGroup, currentTree: Token, flags: BBFlags) {
        when (currentTree.type) {
            TOKEN_ROOT -> {
                currentTree.children.forEach {
                    buildViews(currentView, it, flags)
                }
            }

            TOKEN_TEXT -> {
                val view = LayoutInflater.from(context)
                        .inflate(R.layout.layout_bbcode_text, this, false) as TextView
                val text = SpannableString(currentTree.attribute as String)

                if (flags.isBold) {
                    if (flags.isItalic) {
                        view.setTypeface(view.typeface, Typeface.BOLD_ITALIC)
                    } else {
                        view.setTypeface(view.typeface, Typeface.BOLD)
                    }
                } else if (flags.isItalic) {
                    view.setTypeface(view.typeface, Typeface.ITALIC)
                }

                if (flags.isUnderlined) {
                    text.setSpan(UnderlineSpan(), 0, text.length, 0)
                }

                if (flags.textColor != null) {
                    view.setTextColor(Color.parseColor(flags.textColor!!))
                }

                view.textSize = flags.textSize
                view.text = text

                currentView.addView(view)
            }

            TOKEN_SIZE -> {
                currentTree.children.forEach {
                    buildViews(currentView, it, flags.clone().apply {
                        textSize = currentTree.attribute as Float
                    })
                }
            }

            TOKEN_BOLD -> {
                currentTree.children.forEach {
                    buildViews(currentView, it, flags.clone().apply {
                        isBold = true
                    })
                }
            }

            TOKEN_ITALIC -> {
                currentTree.children.forEach {
                    buildViews(currentView, it, flags.clone().apply {
                        isItalic = true
                    })
                }
            }

            TOKEN_UNDERLINE -> {
                currentTree.children.forEach {
                    buildViews(currentView, it, flags.clone().apply {
                        isUnderlined = true
                    })
                }
            }

            TOKEN_LEFT -> {
                val container = LayoutInflater.from(context)
                        .inflate(R.layout.layout_bbcode_container, this, false) as FlowLayout

                container.gravity = Gravity.START

                currentTree.children.forEach {
                    buildViews(container, it, flags.clone())
                }

                currentView.addView(container)
            }

            TOKEN_CENTER -> {
                val container = LayoutInflater.from(context)
                        .inflate(R.layout.layout_bbcode_container, this, false) as FlowLayout

                container.gravity = Gravity.CENTER_HORIZONTAL

                currentTree.children.forEach {
                    buildViews(container, it, flags.clone())
                }

                currentView.addView(container)
            }

            TOKEN_RIGHT -> {
                val container = LayoutInflater.from(context)
                        .inflate(R.layout.layout_bbcode_container, this, false) as FlowLayout

                container.gravity = Gravity.END

                currentTree.children.forEach {
                    buildViews(container, it, flags.clone())
                }

                currentView.addView(container)
            }

            TOKEN_COLOR -> {
                currentTree.children.forEach {
                    buildViews(currentView, it, flags.clone().apply {
                        textColor = currentTree.attribute as String
                    })
                }
            }
        }
    }

    private fun tokenize(input: String): Token {
        val result = Token(TOKEN_ROOT)
        var currentToken = result
        val bbStack = Stack<BBToken>()
        var index = 0

        while (index < input.length) {
            if (input[index] == '[') {
                val segmentToAnalyse = input.substring(index)
                var matchFound = false

                if (bbStack.isNotEmpty() && bbStack.peek().isEnd(segmentToAnalyse)) {
                    index += bbStack.pop().endLength
                    currentToken = currentToken.parent!!

                    continue
                }

                for (it in bbTokens) {
                    if (it.isStart(segmentToAnalyse)) {
                        val token = it.createFrom(segmentToAnalyse)

                        token.parent = currentToken
                        currentToken.children.add(token)
                        currentToken = token
                        bbStack.push(it)

                        index += it.startLength
                        matchFound = true

                        break
                    }
                }

                if (!matchFound) {
                    val text = getText(input, index, true)
                    val token = Token(TOKEN_TEXT, text)

                    token.parent = currentToken
                    currentToken.children.add(token)
                    index += text.length
                }
            } else {
                val text = getText(input, index)
                val token = Token(TOKEN_TEXT, text)

                token.parent = currentToken
                currentToken.children.add(token)
                index += text.length
            }
        }

        return result
    }

    private fun getText(input: String, index: Int, literalBracket: Boolean = false): String {
        var subIndex = index + if (literalBracket) 1 else 0

        while (subIndex < input.length) {
            if (input[subIndex] == '[') {
                return input.substring(index, subIndex)
            } else {
                subIndex++
            }
        }

        return input.substring(index, subIndex)
    }

    private class Token(val type: Int, val attribute: Any? = null) {
        var parent: Token? = null
        val children = ArrayList<Token>()
    }

    private class BBFlags {
        var textSize: Float = 14f
        var textColor: String? = null
        var isBold = false
        var isItalic = false
        var isUnderlined = false
        var gravity: Int? = null

        constructor()

        private constructor(textSize: Float, isBold: Boolean) {
            this.isBold = isBold
            this.textSize = textSize
        }

        fun clone(): BBFlags {
            return BBFlags(textSize, isBold)
        }
    }

    private abstract class BBToken() {

        abstract val startLength: Int
        abstract val endLength: Int

        abstract fun isStart(segment: String): Boolean

        abstract fun isEnd(segment: String): Boolean

        abstract fun createFrom(segment: String): Token
    }

    private class SizeToken : BBToken() {

        override val startLength = 8
        override val endLength = 7

        override fun isStart(segment: String) = segment.startsWith("[SIZE=1]", true) ||
                segment.startsWith("[SIZE=2]", true) || segment.startsWith("[SIZE=3]", true) ||
                segment.startsWith("[SIZE=4]", true) || segment.startsWith("[SIZE=5]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/SIZE]")

        override fun createFrom(segment: String) =
                Token(TOKEN_SIZE, when (segment.substring(6, 7).toInt()) {
                    1 -> 10f
                    2 -> 12f
                    3 -> 14f
                    4 -> 16f
                    5 -> 18f
                    else -> 14f
                })
    }

    private class BoldToken : BBToken() {

        override val startLength = 3
        override val endLength = 4

        override fun isStart(segment: String) = segment.startsWith("[B]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/B]", true)

        override fun createFrom(segment: String) = Token(TOKEN_BOLD)
    }

    private class ItalicToken : BBToken() {

        override val startLength = 3
        override val endLength = 4

        override fun isStart(segment: String) = segment.startsWith("[I]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/I]", true)

        override fun createFrom(segment: String) = Token(TOKEN_ITALIC)
    }

    private class UnderlineToken : BBToken() {

        override val startLength = 3
        override val endLength = 4

        override fun isStart(segment: String) = segment.startsWith("[U]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/U]", true)

        override fun createFrom(segment: String) = Token(TOKEN_UNDERLINE)
    }

    private class LeftToken : BBToken() {

        override val startLength = 6
        override val endLength = 7

        override fun isStart(segment: String) = segment.startsWith("[LEFT]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/LEFT]", true)

        override fun createFrom(segment: String) = Token(TOKEN_LEFT)
    }

    private class CenterToken : BBToken() {

        override val startLength = 8
        override val endLength = 9

        override fun isStart(segment: String) = segment.startsWith("[CENTER]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/CENTER]", true)

        override fun createFrom(segment: String) = Token(TOKEN_CENTER)
    }

    private class RightToken : BBToken() {

        override val startLength = 7
        override val endLength = 8

        override fun isStart(segment: String) = segment.startsWith("[RIGHT]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/RIGHT]", true)

        override fun createFrom(segment: String) = Token(TOKEN_RIGHT)
    }

    private class ColorToken : BBToken() {

        override val startLength = 15
        override val endLength = 8

        override fun isStart(segment: String) =
                segment.matches(Regex("^\\[COLOR=#[0-9a-f]{6}\\].*$", setOf(RegexOption.IGNORE_CASE,
                        RegexOption.DOT_MATCHES_ALL)))

        override fun isEnd(segment: String) = segment.startsWith("[/COLOR]", true)

        override fun createFrom(segment: String) =
                Token(TOKEN_COLOR, segment.substring(7, 14))
    }
}