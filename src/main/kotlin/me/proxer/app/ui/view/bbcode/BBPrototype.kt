package me.proxer.app.ui.view.bbcode

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

    fun construct(code: String, parent: BBTree): BBTree
}
