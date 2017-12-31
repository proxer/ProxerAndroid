@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.ui.view.bbcode

import android.text.SpannableStringBuilder
import android.view.View
import org.jetbrains.anko.childrenRecursiveSequence

inline fun <reified T> applyToViews(views: List<View>, operation: (T) -> Unit): List<View> {
    return views.map { view ->
        if (view is T) {
            operation(view)
        } else {
            view.childrenRecursiveSequence().plus(view)
                    .filterIsInstance(T::class.java)
                    .forEach(operation)
        }

        view
    }
}

inline fun CharSequence.toSpannableStringBuilder() = this as? SpannableStringBuilder
        ?: SpannableStringBuilder(this)

inline fun SpannableStringBuilder.trimStartSafely() = when (firstOrNull()?.isWhitespace()) {
    true -> indices
            .firstOrNull { !this[it].isWhitespace() }
            ?.let { delete(0, it) }
            ?: apply { clear() }
    else -> this
}

inline fun SpannableStringBuilder.trimEndSafely() = when (lastOrNull()?.isWhitespace()) {
    true -> indices.reversed()
            .firstOrNull { !this[it].isWhitespace() }
            ?.let { delete(it + 1, length) }
            ?: apply { clear() }
    else -> this
}
