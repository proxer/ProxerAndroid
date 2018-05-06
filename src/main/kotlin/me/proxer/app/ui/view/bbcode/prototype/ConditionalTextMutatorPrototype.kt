package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.BBTree

/**
 * @author Ruben Gees
 */
interface ConditionalTextMutatorPrototype : TextMutatorPrototype {

    fun canOptimize(recursiveChildren: List<BBTree>) = recursiveChildren.none {
        it.prototype !== TextPrototype && it.prototype !is TextMutatorPrototype
    }
}
