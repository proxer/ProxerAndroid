package me.proxer.app.ui.view.bbcode.code

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object CodePrototype : BBPrototype {

    override val startRegex = Regex(" *code( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *code *", BBPrototype.REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = CodeTree(parent)
}
