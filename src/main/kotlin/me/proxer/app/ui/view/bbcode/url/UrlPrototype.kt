package me.proxer.app.ui.view.bbcode.url

import android.util.Patterns
import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object UrlPrototype : BBPrototype {

    override val startRegex = Regex("\\s*url\\s*(=\\s*\"?${Patterns.WEB_URL}\"?\\s*)?", RegexOption.IGNORE_CASE)
    override val endRegex = Regex("/\\s*url\\s*", RegexOption.IGNORE_CASE)

    override fun construct(code: String, parent: BBTree): BBTree {
        val url = code.substringAfter("=", "").trim().trim { it == '"' }
        val parsedUrl = HttpUrl.parse(url)

        return UrlTree(parsedUrl ?: ProxerUrls.webBase(), parent) /* parsedUrl being null should never happen,
                                                                         but prevent crash just in case. */
    }
}
