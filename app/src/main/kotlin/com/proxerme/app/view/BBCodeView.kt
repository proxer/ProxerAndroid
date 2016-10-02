package com.proxerme.app.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.proxerme.app.R
import org.jetbrains.anko.find
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class BBCodeView : LinearLayout {

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
        private const val TOKEN_SPOILER = 10

        private val bbTokens = arrayOf(SizeToken(), BoldToken(), ItalicToken(), UnderlineToken(),
                LeftToken(), CenterToken(), RightToken(), ColorToken(), SpoilerToken())
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
            BBTreeProcessor(tokenize(bbCode!!)).buildViews().forEach {
                addView(it)
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

    private inner class BBTreeProcessor(tree: Token) {
        private val entries = LinkedList<BBResultEntry>()

        init {
            traverse(tree, BBResultEntry())
        }

        fun buildViews(): List<View> {
            return merge().map {
                val result: View
                val textView: TextView

                if (it.isSpoiler) {
                    result = LayoutInflater.from(context)
                            .inflate(R.layout.layout_bbcode_spoiler, this@BBCodeView, false)
                            as ViewGroup
                    textView = result.find(R.id.text)
                    val spoilerButton = result.find<Button>(R.id.spoilerButton)

                    spoilerButton.setOnClickListener {
                        if (textView.visibility == View.VISIBLE) {
                            textView.visibility = View.GONE
                            spoilerButton.text = "Spoiler!"
                        } else {
                            textView.visibility = View.VISIBLE
                            spoilerButton.text = "Spoiler verstecken"
                        }
                    }
                } else {
                    textView = LayoutInflater.from(context)
                            .inflate(R.layout.layout_bbcode_text, this@BBCodeView, false)
                            as TextView
                    result = textView
                }

                textView.text = it.styledText
                textView.gravity = it.gravity

                result
            }
        }

        private fun merge(): List<BBViewConfiguration> {
            val result = LinkedList<BBViewConfiguration>()
            val compatibleSpannables = LinkedList<SpannableString>()

            for (i in 0 until entries.size) {
                val entry = entries[i]

                if (entry.text != null) {
                    val text = SpannableString(entry.text)

                    text.setSpan(RelativeSizeSpan(entry.size), 0, entry.text!!.length,
                            Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

                    if (entry.isBold) {
                        if (entry.isItalic) {
                            text.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, entry.text!!.length,
                                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                        } else {
                            text.setSpan(StyleSpan(Typeface.BOLD), 0, entry.text!!.length,
                                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                        }
                    } else if (entry.isItalic) {
                        text.setSpan(StyleSpan(Typeface.ITALIC), 0, entry.text!!.length,
                                Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                    }

                    if (entry.isUnderlined) {
                        text.setSpan(UnderlineSpan(), 0, entry.text!!.length,
                                Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                    }

                    if (entry.color != null) {
                        text.setSpan(ForegroundColorSpan(Color.parseColor(entry.color)), 0,
                                entry.text!!.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                    }

                    compatibleSpannables.add(text)
                }

                if (i + 1 >= entries.size || entries[i + 1].gravity != entry.gravity ||
                        entries[i + 1].isSpoiler != entry.isSpoiler) {
                    result.add(BBViewConfiguration(TextUtils
                            .concat(*compatibleSpannables.toTypedArray()), entry.gravity,
                            entry.isSpoiler))

                    compatibleSpannables.clear()
                }
            }

            return result
        }

        private fun traverse(tree: Token, currentEntry: BBResultEntry) {
            if (tree.type == TOKEN_TEXT) {
                currentEntry.text = tree.attribute as String

                entries.add(currentEntry)
            } else {
                tree.children.forEach {
                    traverse(it, applyStyle(tree, currentEntry.clone()))
                }
            }
        }

        private fun applyStyle(token: Token, entry: BBResultEntry): BBResultEntry {
            when (token.type) {
                TOKEN_SIZE -> entry.size = token.attribute as Float
                TOKEN_BOLD -> entry.isBold = true
                TOKEN_ITALIC -> entry.isItalic = true
                TOKEN_UNDERLINE -> entry.isUnderlined = true
                TOKEN_LEFT -> entry.gravity = Gravity.START
                TOKEN_CENTER -> entry.gravity = Gravity.CENTER
                TOKEN_RIGHT -> entry.gravity = Gravity.END
                TOKEN_COLOR -> entry.color = token.attribute as String
                TOKEN_SPOILER -> entry.isSpoiler = true
            }

            return entry
        }
    }

    private class BBResultEntry {
        var text: String? = null
        var gravity = Gravity.START
        var size = 1.0f
        var color: String? = null
        var isBold = false
        var isItalic = false
        var isUnderlined = false
        var isSpoiler = false

        constructor()

        private constructor(text: String?, gravity: Int, size: Float, color: String?,
                            isBold: Boolean, isItalic: Boolean, isUnderlined: Boolean,
                            isSpoiler: Boolean) {
            this.text = text
            this.gravity = gravity
            this.size = size
            this.color = color
            this.isBold = isBold
            this.isItalic = isItalic
            this.isUnderlined = isUnderlined
            this.isSpoiler = isSpoiler
        }


        fun clone(): BBResultEntry {
            return BBResultEntry(text, gravity, size, color, isBold, isItalic, isUnderlined,
                    isSpoiler)
        }
    }

    private class BBViewConfiguration(val styledText: CharSequence, val gravity: Int,
                                      val isSpoiler: Boolean)

    private class Token(val type: Int, val attribute: Any? = null) {
        var parent: Token? = null
        val children = ArrayList<Token>()
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
                    1 -> 0.6f
                    2 -> 0.8f
                    3 -> 1.0f
                    4 -> 1.2f
                    5 -> 1.4f
                    else -> 1.0f
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

    private class SpoilerToken : BBToken() {

        override val startLength = 9
        override val endLength = 10

        override fun isStart(segment: String) = segment.startsWith("[SPOILER]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/SPOILER]", true)

        override fun createFrom(segment: String) = Token(TOKEN_SPOILER)
    }
}