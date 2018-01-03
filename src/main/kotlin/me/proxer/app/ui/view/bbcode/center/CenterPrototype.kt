package me.proxer.app.ui.view.bbcode.center

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object CenterPrototype : BBPrototype {

    override val startRegex = Regex(" *center( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *center *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = CenterTree(parent)
}