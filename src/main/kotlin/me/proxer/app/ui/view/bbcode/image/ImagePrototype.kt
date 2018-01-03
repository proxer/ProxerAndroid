package me.proxer.app.ui.view.bbcode.image

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils

/**
 * @author Ruben Gees
 */
object ImagePrototype : BBPrototype {

    override val startRegex = Regex(" *img( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *img *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val width = BBUtils.cutAttribute(code, "size=")?.toIntOrNull()

        return ImageTree(width, parent)
    }
}
