package com.proxerme.app.view.bbcode

import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object BBTokenizer {

    const val TOKEN_ROOT = 0
    const val TOKEN_TEXT = 1
    const val TOKEN_SIZE = 2
    const val TOKEN_BOLD = 3
    const val TOKEN_ITALIC = 4
    const val TOKEN_UNDERLINE = 5
    const val TOKEN_LEFT = 6
    const val TOKEN_CENTER = 7
    const val TOKEN_RIGHT = 8
    const val TOKEN_COLOR = 9
    const val TOKEN_SPOILER = 10

    private val bbTokens = arrayOf(SizeToken, BoldToken, ItalicToken, UnderlineToken, LeftToken,
            CenterToken, RightToken, ColorToken, SpoilerToken)

    fun tokenize(input: String): BBToken {
        val result = BBToken(TOKEN_ROOT)
        var currentToken = result
        val bbStack = Stack<InternalBBToken>()
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
                    val token = BBToken(TOKEN_TEXT, text)

                    token.parent = currentToken
                    currentToken.children.add(token)
                    index += text.length
                }
            } else {
                val text = getText(input, index)
                val token = BBToken(TOKEN_TEXT, text)

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

    class BBToken(val type: Int, val attribute: Any? = null) {
        var parent: BBToken? = null
        val children = ArrayList<BBToken>()
    }

    private abstract class InternalBBToken() {

        abstract val startLength: Int
        abstract val endLength: Int

        abstract fun isStart(segment: String): Boolean

        abstract fun isEnd(segment: String): Boolean

        abstract fun createFrom(segment: String): BBToken
    }

    private object SizeToken : InternalBBToken() {

        override val startLength = 8
        override val endLength = 7

        override fun isStart(segment: String) = segment.startsWith("[SIZE=1]", true) ||
                segment.startsWith("[SIZE=2]", true) || segment.startsWith("[SIZE=3]", true) ||
                segment.startsWith("[SIZE=4]", true) || segment.startsWith("[SIZE=5]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/SIZE]")

        override fun createFrom(segment: String) =
                BBToken(TOKEN_SIZE, when (segment.substring(6, 7).toInt()) {
                    1 -> 0.6f
                    2 -> 0.8f
                    3 -> 1.0f
                    4 -> 1.2f
                    5 -> 1.4f
                    else -> 1.0f
                })
    }

    private object BoldToken : InternalBBToken() {

        override val startLength = 3
        override val endLength = 4

        override fun isStart(segment: String) = segment.startsWith("[B]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/B]", true)

        override fun createFrom(segment: String) = BBToken(TOKEN_BOLD)
    }

    private object ItalicToken : InternalBBToken() {

        override val startLength = 3
        override val endLength = 4

        override fun isStart(segment: String) = segment.startsWith("[I]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/I]", true)

        override fun createFrom(segment: String) = BBToken(TOKEN_ITALIC)
    }

    private object UnderlineToken : InternalBBToken() {

        override val startLength = 3
        override val endLength = 4

        override fun isStart(segment: String) = segment.startsWith("[U]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/U]", true)

        override fun createFrom(segment: String) = BBToken(TOKEN_UNDERLINE)
    }

    private object LeftToken : InternalBBToken() {

        override val startLength = 6
        override val endLength = 7

        override fun isStart(segment: String) = segment.startsWith("[LEFT]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/LEFT]", true)

        override fun createFrom(segment: String) = BBToken(TOKEN_LEFT)
    }

    private object CenterToken : InternalBBToken() {

        override val startLength = 8
        override val endLength = 9

        override fun isStart(segment: String) = segment.startsWith("[CENTER]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/CENTER]", true)

        override fun createFrom(segment: String) = BBToken(TOKEN_CENTER)
    }

    private object RightToken : InternalBBToken() {

        override val startLength = 7
        override val endLength = 8

        override fun isStart(segment: String) = segment.startsWith("[RIGHT]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/RIGHT]", true)

        override fun createFrom(segment: String) = BBToken(TOKEN_RIGHT)
    }

    private object ColorToken : InternalBBToken() {

        override val startLength = 15
        override val endLength = 8

        override fun isStart(segment: String) =
                segment.matches(Regex("^\\[COLOR=#[0-9a-f]{6}\\].*$", setOf(RegexOption.IGNORE_CASE,
                        RegexOption.DOT_MATCHES_ALL)))

        override fun isEnd(segment: String) = segment.startsWith("[/COLOR]", true)

        override fun createFrom(segment: String) =
                BBToken(TOKEN_COLOR, segment.substring(7, 14))
    }

    private object SpoilerToken : InternalBBToken() {

        override val startLength = 9
        override val endLength = 10

        override fun isStart(segment: String) = segment.startsWith("[SPOILER]", true)

        override fun isEnd(segment: String) = segment.startsWith("[/SPOILER]", true)

        override fun createFrom(segment: String) = BBToken(TOKEN_SPOILER)
    }
}