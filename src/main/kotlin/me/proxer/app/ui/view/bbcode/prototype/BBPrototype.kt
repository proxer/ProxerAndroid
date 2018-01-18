package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.View
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.ui.view.bbcode.trimEndSafely
import me.proxer.app.ui.view.bbcode.trimStartSafely
import org.jetbrains.anko.collections.forEachWithIndex
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

        val result = mutableListOf<View>()
        val currentTextViews = mutableListOf<TextView>()

        childViews.forEachWithIndex { index, childView ->
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
                            result.add(TextPrototype.makeView(context, ""))
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
                    result.add(TextPrototype.makeView(context, ""))
                }
            }
        }

        if (currentTextViews.isNotEmpty()) {
            // Don't pad if this view does only consist of TextViews.
            val shouldPadStart = currentTextViews.size != childViews.size

            result.add(mergeAndTrim(currentTextViews, shouldPadStart, false))

            currentTextViews.clear()
        }

        return result
    }

    private fun mergeAndTrim(views: List<TextView>, padStart: Boolean, padEnd: Boolean): TextView {
        val current = views.first()
        val currentText = current.text.toSpannableStringBuilder()

        for (currentTextView in views.drop(1)) {
            currentText.append(currentTextView.text)
        }

        currentText.trimStartSafely()
        currentText.trimEndSafely()

        if (padStart) currentText.insert(0, "\n")
        if (padEnd) currentText.append("\n")

        current.text = currentText

        return current
    }
}
