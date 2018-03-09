@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.ui.view.bbcode

import android.text.SpannableStringBuilder
import android.view.View
import me.proxer.app.R
import org.jetbrains.anko.childrenRecursiveSequence

/**
 * @author Ruben Gees
 */
internal object BBUtils {

    internal fun cutAttribute(target: String, regex: Regex): String? {
        return regex.find(target)?.groupValues?.getOrNull(1)?.trim()?.trim { it == '"' }
    }
}

internal inline fun <reified T : View> applyToViews(views: List<View>, operation: (T) -> Unit) = views.apply {
    flatMap { it.childrenRecursiveSequence().plus(it).toList() }
        .filterIsInstance(T::class.java)
        .filter { it.getTag(R.id.ignore_tag) == null }
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
