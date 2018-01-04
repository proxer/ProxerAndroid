package me.proxer.app.ui.view.bbcode.list

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
object ListItemPrototype : BBPrototype {

    override val startRegex = Regex(" *li( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *li *", BBPrototype.REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = ListItemTree(parent)
}
