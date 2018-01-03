package me.proxer.app.ui.view.bbcode.quote

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

object QuotePrototype : BBPrototype {

    override val startRegex = Regex(" *quote(=\"?.*?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *quote *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        return QuoteTree(parent)
    }
}
