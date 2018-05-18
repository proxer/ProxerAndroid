package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.UrlClickableSpan
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
object AttachmentPrototype : ConditionalTextMutatorPrototype, AutoClosingPrototype {

    private val IMAGE_EXTENSIONS = arrayOf("png", "jpg", "jpeg", "gif")
    private val WHITESPACE_REGEX = Regex("\\s")

    override val startRegex = Regex(" *attachment( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *attachment *", REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(context, args) }

        if (childViews.isEmpty()) {
            return childViews
        }

        val attachment = (childViews.firstOrNull() as? TextView)?.text.toString().trim()

        return when {
            isImage(attachment) -> {
                val parent = children.first().parent ?: throw IllegalArgumentException("parent is null")
                val url = constructUrl(args.safeUserId, attachment)

                ImagePrototype.makeViews(context, listOf(TextPrototype.construct(url.toString(), parent)), args)
            }
            else -> applyToViews<TextView>(childViews) {
                it.text = mutate(it.text.toSpannableStringBuilder(), args)
            }
        }
    }

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val url = constructUrl(args.safeUserId, text)

        return text.toSpannableStringBuilder().apply {
            replace(0, length, globalContext.getString(R.string.view_bbcode_attachment_link))

            setSpan(UrlClickableSpan(url), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    override fun canOptimize(recursiveChildren: List<BBTree>): Boolean {
        val firstChild = recursiveChildren.firstOrNull()

        return if (firstChild?.prototype === TextPrototype) {
            !isImage(firstChild.args.safeText) && super.canOptimize(recursiveChildren)
        } else {
            false
        }
    }

    private fun isImage(attachment: CharSequence) = IMAGE_EXTENSIONS.any { attachment.endsWith(it, true) }

    private fun constructUrl(userId: String, attachment: CharSequence) = ProxerUrls.webBase().newBuilder()
        .addPathSegments("media/kunena/attachments/$userId/${attachment.replace(WHITESPACE_REGEX, "")}")
        .build()
}
