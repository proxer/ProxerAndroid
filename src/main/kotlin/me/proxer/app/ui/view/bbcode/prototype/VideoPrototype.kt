package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object VideoPrototype : BBPrototype {

    private const val DELIMITER = "type="
    private const val TYPE_ARGUMENT = "type"
    private const val TYPE_YOUTUBE = "youtube"
    private const val TYPE_YOUTUBE_URL = "https://youtu.be/"

    override val startRegex = Regex(" *video( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *video *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val type = BBUtils.cutAttribute(code, DELIMITER)

        return BBTree(this, parent, args = mapOf(TYPE_ARGUMENT to type))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }.filterIsInstance(TextView::class.java)
        val text = context.getString(R.string.view_bbcode_video_link)

        val urlOrId = childViews.firstOrNull()?.text?.toString() ?: ""
        val type = args[TYPE_ARGUMENT] as String?

        val url = HttpUrl.parse(when {
            type?.equals(TYPE_YOUTUBE, ignoreCase = true) == true -> "$TYPE_YOUTUBE_URL$urlOrId"
            else -> urlOrId
        })

        return when (url) {
            null -> childViews
            else -> applyToViews(childViews, { view: TextView ->
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View?) {
                        (context as? BaseActivity)?.showPage(url)
                    }
                }

                view.text = text.toSpannableStringBuilder().apply {
                    setSpan(clickableSpan, 0, text.length, SPAN_INCLUSIVE_EXCLUSIVE)
                }
            })
        }
    }
}
