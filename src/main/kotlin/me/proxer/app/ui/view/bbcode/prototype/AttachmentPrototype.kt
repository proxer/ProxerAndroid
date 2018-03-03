package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.text.Spannable
import android.view.View
import android.widget.TextView
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBTree.Companion.USER_ID_ARGUMENT
import me.proxer.app.ui.view.bbcode.UrlClickableSpan
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
object AttachmentPrototype : AutoClosingPrototype {

    private val IMAGE_EXTENSIONS = arrayOf("png", "jpg", "jpeg", "gif")

    override val startRegex = Regex(" *attachment( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *attachment *", REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }

        if (childViews.isEmpty()) {
            return childViews
        }

        val userId = args[USER_ID_ARGUMENT] as String? ?: throw IllegalStateException("userId is null")
        val attachment = (childViews.firstOrNull() as? TextView)?.text.toString().trim()
        val url = constructUrl(userId, attachment)

        return when {
            isImage(attachment) -> {
                val parent = children.first().parent ?: throw IllegalArgumentException("parent is null")

                ImagePrototype.makeViews(context, listOf(TextPrototype.construct(url.toString(), parent)), args)
            }
            else -> applyToViews<TextView>(childViews, {
                globalContext.getString(R.string.view_bbcode_attachment_link).toSpannableStringBuilder().apply {
                    setSpan(UrlClickableSpan(url), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            })
        }
    }

    private fun isImage(attachment: CharSequence) = IMAGE_EXTENSIONS.any { attachment.endsWith(it, true) }

    private fun constructUrl(userId: String, attachment: CharSequence) = ProxerUrls.webBase().newBuilder()
            .addPathSegments("media/kunena/attachments/$userId/$attachment")
            .build()
}
