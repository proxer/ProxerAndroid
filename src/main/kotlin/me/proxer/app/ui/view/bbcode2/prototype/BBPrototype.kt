package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree

/**
 * @author Ruben Gees
 */
interface BBPrototype {

    fun fromCode(code: String, parent: BBTree): BBTree?
}
