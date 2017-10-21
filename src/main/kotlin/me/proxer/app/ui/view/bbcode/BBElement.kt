package me.proxer.app.ui.view.bbcode

import android.text.SpannableStringBuilder

internal sealed class BBElement {
    internal class BBTextElement(val text: SpannableStringBuilder, val gravity: Int) : BBElement() {

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
