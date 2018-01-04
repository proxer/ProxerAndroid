package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import me.proxer.app.base.BaseActivity
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.Utils
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object UrlPrototype : BBPrototype {

    private const val DELIMITER = "url="
    private const val URL_ARGUMENT = "url"

    private val INVALID_URL = HttpUrl.parse("https://proxer.me/404")
            ?: throw IllegalArgumentException("Could not parse url")

    override val startRegex = Regex(" *url(=\"?.*?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *url *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val url = BBUtils.cutAttribute(code, DELIMITER) ?: ""
        val parsedUrl = Utils.safelyParseAndFixUrl(url) ?: INVALID_URL

        return BBTree(this, parent, args = mapOf(URL_ARGUMENT to parsedUrl))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }
        val url = args[URL_ARGUMENT] as HttpUrl

        return applyToViews(childViews, { view: TextView ->
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View?) {
                    (context as? BaseActivity)?.showPage(url)
                }
            }

            view.text = view.text.toSpannableStringBuilder().apply {
                setSpan(clickableSpan, 0, view.length(), SPAN_INCLUSIVE_EXCLUSIVE)
            }
        })
    }
}
