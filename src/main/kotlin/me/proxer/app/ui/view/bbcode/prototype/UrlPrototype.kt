package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.applyToAllViews
import me.proxer.app.ui.view.bbcode.linkifyUrl
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.Utils
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object UrlPrototype : ConditionalTextMutatorPrototype, AutoClosingPrototype {

    private val ATTRIBUTE_REGEX = Regex("url *= *(.+?)( |$)", REGEX_OPTIONS)
    private const val URL_ARGUMENT = "url"

    private val INVALID_URL = HttpUrl.get("https://proxer.me/404")

    override val startRegex = Regex(" *url *= *.+?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *url *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val url = BBUtils.cutAttribute(code, ATTRIBUTE_REGEX)?.trim() ?: ""
        val parsedUrl = Utils.parseAndFixUrl(url) ?: INVALID_URL

        return BBTree(this, parent, args = BBArgs(custom = *arrayOf(URL_ARGUMENT to parsedUrl)))
    }

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(parent, args) }
        val url = args[URL_ARGUMENT] as HttpUrl

        return applyToAllViews(childViews) { view: View ->
            when (view) {
                is TextView -> view.text = mutate(view.text.toSpannableStringBuilder(), args)
                else -> view.clicks()
                    .autoDisposable(ViewScopeProvider.from(parent))
                    .subscribe { BBUtils.findBaseActivity(view.context)?.showPage(url) }
            }
        }
    }

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val url = args[URL_ARGUMENT] as HttpUrl

        return text.linkifyUrl(url)
    }
}
