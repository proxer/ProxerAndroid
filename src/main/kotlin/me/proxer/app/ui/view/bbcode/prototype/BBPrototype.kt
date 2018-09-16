package me.proxer.app.ui.view.bbcode.prototype

import android.view.View
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
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

    fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(parent, args) }

        val currentTextViews = mutableListOf<TextView>()
        val result = mutableListOf<View>()

        childViews.withIndex().forEach { (index, childView) ->
            if (childView is TextView) {
                currentTextViews.add(childView)
            } else {
                if (currentTextViews.isNotEmpty()) {
                    // Don't pad start if the currentTextViews are the first.
                    val shouldPadStart = index - currentTextViews.size > 0
                    val mergedView = mergeAndTrim(currentTextViews, shouldPadStart, true)

                    // Add a single line padding view instead of the potentially more padded mergedView if blank.
                    if (mergedView.text.isBlank()) {
                        // Only add if it is not the first view.
                        if (shouldPadStart) {
                            result.add(TextPrototype.makeView(parent, args + BBArgs(text = "")))
                        }
                    } else {
                        result.add(mergedView)
                    }

                    currentTextViews.clear()
                }

                result.add(childView)

                // Add a padding view if the next view is also not a TextView.
                val isBetweenNonTextViews = index + 1 <= childViews.lastIndex && childViews[index + 1] !is TextView

                if (isBetweenNonTextViews) {
                    result.add(TextPrototype.makeView(parent, args + BBArgs(text = "")))
                }
            }
        }

        if (currentTextViews.isNotEmpty()) {
            // Don't pad if this view does only consist of TextViews.
            val shouldPadStart = currentTextViews.size != childViews.size
            val mergedView = mergeAndTrim(currentTextViews, shouldPadStart, false)

            if (mergedView.text.isNotBlank()) {
                result.add(mergedView)
            }
        }

        return result
    }

    private fun mergeAndTrim(views: List<TextView>, padStart: Boolean, padEnd: Boolean): TextView {
        val current = views.first()
        val currentText = current.text.toSpannableStringBuilder()

        for (currentTextView in views.drop(1)) {
            currentText.append(currentTextView.text)
        }

        currentText.trimStartSafely().trimEndSafely()

        if (padStart) currentText.insert(0, "\n")
        if (padEnd) currentText.append("\n")

        current.text = currentText

        return current
    }
}
