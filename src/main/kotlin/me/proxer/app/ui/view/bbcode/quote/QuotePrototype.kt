package me.proxer.app.ui.view.bbcode.quote

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree

object QuotePrototype : BBPrototype {

    override val startRegex = Regex("\\s*quote\\s*", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*quote\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree): BBTree {
        return QuoteTree(parent)
    }
}
