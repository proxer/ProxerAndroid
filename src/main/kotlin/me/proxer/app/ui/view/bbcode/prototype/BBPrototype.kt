package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.tree.BBTree

/**
 * @author Ruben Gees
 */
interface BBPrototype {

    fun fromCode(code: String, parent: BBTree): BBTree?
}
