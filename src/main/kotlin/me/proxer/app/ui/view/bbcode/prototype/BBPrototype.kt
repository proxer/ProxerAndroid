package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.View
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.ui.view.bbcode.trimEndSafely
import me.proxer.app.ui.view.bbcode.trimStartSafely
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * @author Ruben Gees
 */
interface BBPrototype {

    companion object {
        val REGEX_OPTIONS = setOf(IGNORE_CASE, DOT_MATCHES_ALL)
    }

    val startRegex: Regex
    val endRegex: Regex

    val canHaveChildren get() = true

    fun fromCode(code: String, parent: BBTree) = when (startRegex.matches(code)) {
        true -> construct(code, parent)
        false -> null
    }

    fun construct(code: String, parent: BBTree) = BBTree(this, parent)

    fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }

        if (childViews.isEmpty()) return childViews

        val result = mutableListOf<View>()
        var current = childViews.first()

        if (current is TextView) {
            current.text = current.text.toSpannableStringBuilder().trimStartSafely()
        }

        for (next in childViews.drop(1)) {
            if (current is TextView) {
                val currentText = current.text.toSpannableStringBuilder()

                if (next is TextView) {
                    current.text = currentText
                            .apply { if (isEmpty()) insert(0, "\n") }
                            .insert(currentText.length, next.text)
                } else {
                    current.text = currentText
                            .trimEndSafely()
                            .apply { if (isNotEmpty()) insert(length, "\n") }

                    result += current
                    current = next
                }
            } else {
                if (next is TextView) {
                    val nextText = next.text.toSpannableStringBuilder()

                    next.text = nextText
                            .trimStartSafely()
                            .apply { if (isNotEmpty()) insert(0, "\n") }

                    result += current
                } else {
                    result += current
                    result += TextPrototype.makeView(context, "")
                }

                current = next
            }
        }

        if (current is TextView) {
            current.text = current.text.toSpannableStringBuilder().trimEndSafely()

            if (current.text.isNotEmpty()) {
                result += current
            }
        } else {
            result += current
        }

        return result
    }
}
