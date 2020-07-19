@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.content.ContextWrapper
import android.text.SpannableStringBuilder
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import androidx.core.text.util.LinkifyCompat
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype
import me.proxer.app.util.extension.recursiveChildren
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
internal object BBUtils {

    internal fun cutAttribute(target: String, regex: Regex): String? {
        return regex.find(target)?.groupValues?.getOrNull(1)?.trim()?.trim { it == '"' }
    }

    internal fun findBaseActivity(currentContext: Context): BaseActivity? = when (currentContext) {
        is BaseActivity -> currentContext
        is ContextWrapper -> findBaseActivity(currentContext.baseContext)
        else -> null
    }
}

internal val MATCH_ALL_PATTERN = Regex(".*", BBPrototype.REGEX_OPTIONS).toPattern()

internal inline fun applyToAllViews(views: List<View>, noinline operation: (View) -> Unit) = views.apply {
    asSequence()
        .flatMap { sequenceOf(it) + ((it as? ViewGroup)?.recursiveChildren ?: emptySequence()) }
        .filter { it.getTag(R.id.ignore_tag) == null }
        .forEach(operation)
}

internal inline fun <reified T : View> applyToViews(
    views: List<View>,
    noinline operation: (T) -> Unit
) = views.apply {
    asSequence()
        .flatMap { sequenceOf(it) + ((it as? ViewGroup)?.recursiveChildren ?: emptySequence()) }
        .filterIsInstance(T::class.java)
        .filter { it.getTag(R.id.ignore_tag) == null }
        .forEach(operation)
}

internal inline fun CharSequence.toSpannableStringBuilder() = this as? SpannableStringBuilder
    ?: SpannableStringBuilder(this)

internal fun SpannableStringBuilder.trimStartSafely() = when (firstOrNull()?.isWhitespace()) {
    true ->
        indices
            .firstOrNull { !this[it].isWhitespace() }
            ?.let { delete(0, it) }
            ?: apply { clear() }
    else -> this
}

internal fun SpannableStringBuilder.trimEndSafely() = when (lastOrNull()?.isWhitespace()) {
    true ->
        indices.reversed()
            .firstOrNull { !this[it].isWhitespace() }
            ?.let { delete(it + 1, length) }
            ?: apply { clear() }
    else -> this
}

internal inline fun SpannableStringBuilder.linkifyUrl(url: HttpUrl): SpannableStringBuilder {
    val transformFilter = Linkify.TransformFilter { _, _ -> url.toString() }

    LinkifyCompat.addLinks(this, MATCH_ALL_PATTERN, null, null, transformFilter)

    return this
}
