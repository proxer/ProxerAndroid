@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.ui.view.bbcode

import android.text.SpannableStringBuilder
import android.view.View
import org.jetbrains.anko.childrenRecursiveSequence

/**
 * @author Ruben Gees
 */
internal object BBUtils {

    /**
     * Cuts the relevant info from the given [target] by using the given [startDelimiter] and [endDelimiter].
     * Case is ignored and '"' characters are trimmed from the result. If the [startDelimiter] is not found,
     * null is returned.
     *
     * This is a function which should not used anywhere else as in the BBCode parser.
     */
    internal fun cutAttribute(target: String, startDelimiter: String, endDelimiter: String = " "): String? {
        val startIndex = target.indexOf(startDelimiter, ignoreCase = true)
        val endIndex = target.indexOf(endDelimiter, ignoreCase = true, startIndex = startIndex)

        return when {
            startIndex < 0 -> null
            endIndex < 0 -> target.substring(startIndex + startDelimiter.length, target.length).trim { it == '"' }
            else -> target.substring(startIndex + startDelimiter.length, endIndex).trim { it == '"' }
        }
    }
}

internal inline fun <reified T : View> applyToViews(views: List<View>, operation: (T) -> Unit) = views.apply {
    flatMap { it.childrenRecursiveSequence().plus(it).toList() }
            .filterIsInstance(T::class.java)
            .onEach(operation)
}

internal inline fun CharSequence.toSpannableStringBuilder() = this as? SpannableStringBuilder
        ?: SpannableStringBuilder(this)

internal inline fun SpannableStringBuilder.trimStartSafely() = when (firstOrNull()?.isWhitespace()) {
    true -> indices
            .firstOrNull { !this[it].isWhitespace() }
            ?.let { delete(0, it) }
            ?: apply { clear() }
    else -> this
}

internal inline fun SpannableStringBuilder.trimEndSafely() = when (lastOrNull()?.isWhitespace()) {
    true -> indices.reversed()
            .firstOrNull { !this[it].isWhitespace() }
            ?.let { delete(it + 1, length) }
            ?: apply { clear() }
    else -> this
}
