package me.proxer.app.ui.view.bbcode2.prototype

import me.proxer.app.ui.view.bbcode2.tree.BBTree

/**
 * @author Ruben Gees
 */
abstract class BBPrototype {

    abstract fun fromCode(code: String, parent: BBTree): BBTree?
}
