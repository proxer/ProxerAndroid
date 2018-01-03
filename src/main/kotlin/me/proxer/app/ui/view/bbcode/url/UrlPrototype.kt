package me.proxer.app.ui.view.bbcode.url

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.util.Utils

/**
 * @author Ruben Gees
 */
object UrlPrototype : BBPrototype {

    override val startRegex = Regex(" *url(=\"?.*?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *url *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val url = BBUtils.cutAttribute(code, "url=") ?: ""
        val parsedUrl = Utils.parseAndFixUrl(url)

        return UrlTree(parsedUrl, parent)
    }
}
