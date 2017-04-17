package me.proxer.app.view.bbcode

import me.proxer.app.view.bbcode.BBTokenizer.BBTokenRule.*
import me.proxer.app.view.bbcode.BBTokenizer.BBTokenType.*
import java.util.*

/**
 * @author Ruben Gees
 */
internal object BBTokenizer {

    private val rules = arrayOf(SizeTokenRule(), BoldTokenRule(), ItalicTokenRule(), UnderlineTokenRule(),
            LeftTokenRule(), CenterTokenRule(), RightTokenRule(), ColorTokenRule(), SpoilerTokenRule())

    internal fun tokenize(input: String): BBToken {
        val result = BBToken(TOKEN_ROOT)
        var currentToken = result
        val stack = Stack<BBTokenRule>()
        var index = 0

        while (index < input.length) {
            if (input[index] == '[') {
                val segmentToAnalyse = input.substring(index)
                var matchFound = false

                if (stack.isNotEmpty()) {
                    val endLength = stack.peek().isEnd(segmentToAnalyse)

                    if (endLength > 0) {
                        stack.pop()

                        index += endLength
                        currentToken = currentToken.parent!!

                        continue
                    }
                }

                for (it in rules) {
                    val startLength = it.isStart(segmentToAnalyse)

                    if (startLength > 0) {
                        val token = it.createFrom(segmentToAnalyse)

                        token.parent = currentToken
                        currentToken.children.add(token)
                        currentToken = token
                        stack.push(it)

                        index += startLength
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

    internal enum class BBTokenType {
        TOKEN_ROOT, TOKEN_TEXT, TOKEN_SIZE, TOKEN_BOLD, TOKEN_ITALIC, TOKEN_UNDERLINE, TOKEN_LEFT, TOKEN_CENTER,
        TOKEN_RIGHT, TOKEN_COLOR, TOKEN_SPOILER
    }

    internal class BBToken(val type: BBTokenType, val attribute: Any? = null) {
        var parent: BBToken? = null
        val children = ArrayList<BBToken>()
    }

    internal sealed class BBTokenRule {
        internal class SizeTokenRule : BBTokenRule() {

            override fun isStart(segment: String): Int {
                val isStart = segment.length >= 8 && segment.startsWith("[SIZE=", true)
                        && segment.elementAt(6).isDigit() && segment.elementAt(6) - '0' in 1..6
                        && segment.elementAt(7) == ']'

                return if (isStart) 8 else 0
            }

            override fun isEnd(segment: String) = if (segment.startsWith("[/SIZE]", true)) 7 else 0
            override fun createFrom(segment: String) = BBToken(TOKEN_SIZE, when (segment.substring(6, 7).toInt()) {
                1 -> 0.6f
                2 -> 0.8f
                3 -> 1.0f
                4 -> 1.2f
                5 -> 1.4f
                6 -> 1.6f
                else -> 1.0f
            })
        }

        internal class BoldTokenRule : BBTokenRule() {
            override fun isStart(segment: String) = if (segment.startsWith("[B]", true)) 3 else 0
            override fun isEnd(segment: String) = if (segment.startsWith("[/B]", true)) 4 else 0
            override fun createFrom(segment: String) = BBToken(TOKEN_BOLD)
        }

        internal class ItalicTokenRule : BBTokenRule() {
            override fun isStart(segment: String) = if (segment.startsWith("[I]", true)) 3 else 0
            override fun isEnd(segment: String) = if (segment.startsWith("[/I]", true)) 4 else 0
            override fun createFrom(segment: String) = BBToken(TOKEN_ITALIC)
        }

        internal class UnderlineTokenRule : BBTokenRule() {
            override fun isStart(segment: String) = if (segment.startsWith("[U]", true)) 3 else 0
            override fun isEnd(segment: String) = if (segment.startsWith("[/U]", true)) 4 else 0
            override fun createFrom(segment: String) = BBToken(TOKEN_UNDERLINE)
        }

        internal class LeftTokenRule : BBTokenRule() {
            override fun isStart(segment: String) = if (segment.startsWith("[LEFT]", true)) 6 else 0
            override fun isEnd(segment: String) = if (segment.startsWith("[/LEFT]", true)) 7 else 0
            override fun createFrom(segment: String) = BBToken(TOKEN_LEFT)
        }

        internal class CenterTokenRule : BBTokenRule() {
            override fun isStart(segment: String) = if (segment.startsWith("[CENTER]", true)) 8 else 0
            override fun isEnd(segment: String) = if (segment.startsWith("[/CENTER]", true)) 9 else 0
            override fun createFrom(segment: String) = BBToken(TOKEN_CENTER)
        }

        internal class RightTokenRule : BBTokenRule() {
            override fun isStart(segment: String) = if (segment.startsWith("[RIGHT]", true)) 7 else 0
            override fun isEnd(segment: String) = if (segment.startsWith("[/RIGHT]", true)) 8 else 0
            override fun createFrom(segment: String) = BBToken(TOKEN_RIGHT)
        }

        internal class ColorTokenRule : BBTokenRule() {

            override fun isStart(segment: String): Int {
                if (segment.length >= 15 && segment.startsWith("[COLOR=", true)) {
                    if (segment.elementAt(7) == '"') {
                        return if (segment.length >= 17 && segment.elementAt(8) == '#'
                                && segment.subSequence(9, 15).all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                                && segment.elementAt(15) == '"' && segment.elementAt(16) == ']') 17 else 0
                    } else if (segment.elementAt(7) == '#') {
                        return if (segment.subSequence(8, 14).all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                                && segment.elementAt(14) == ']') 15 else 0
                    } else {
                        return 0
                    }
                } else {
                    return 0
                }
            }

            override fun isEnd(segment: String) = if (segment.startsWith("[/COLOR]", true)) 8 else 0
            override fun createFrom(segment: String) = segment.indexOf('#').let {
                BBToken(TOKEN_COLOR, segment.substring(it, it + 7))
            }
        }

        internal class SpoilerTokenRule : BBTokenRule() {
            override fun isStart(segment: String) = if (segment.startsWith("[SPOILER]", true)) 9 else 0
            override fun isEnd(segment: String) = if (segment.startsWith("[/SPOILER]", true)) 10 else 0
            override fun createFrom(segment: String) = BBToken(TOKEN_SPOILER)
        }

        abstract fun isStart(segment: String): Int
        abstract fun isEnd(segment: String): Int
        abstract fun createFrom(segment: String): BBToken
    }
}