package me.proxer.app.ui.view.bbcode.prototype

/**
 * @author Ruben Gees
 */
object RootPrototype : BBPrototype {

    override val startRegex = Regex("x^")
    override val endRegex = Regex("x^")
}
