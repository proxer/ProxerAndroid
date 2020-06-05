package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.linkifyUrl
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
object AttachmentPrototype : ConditionalTextMutatorPrototype, AutoClosingPrototype {

    private val imageExtensions = arrayOf("png", "jpg", "jpeg", "gif")
    private val whitespaceRegex = Regex("\\s")

    override val startRegex = Regex(" *attachment( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *attachment *", REGEX_OPTIONS)

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.flatMap { it.makeViews(parent, args) }

        if (childViews.isEmpty()) {
            return childViews
        }

        val attachment = (childViews.firstOrNull() as? TextView)?.text.toString().trim()

        return when {
            isImage(attachment) -> {
                val parentTree = requireNotNull(children.first().parent)
                val url = constructUrl(args.safeUserId, attachment)

                ImagePrototype.makeViews(
                    parent,
                    listOf(TextPrototype.construct(url.toString(), parentTree)),
                    args
                )
            }
            else -> applyToViews<TextView>(childViews) {
                it.text = mutate(it.text.toSpannableStringBuilder(), args)
            }
        }
    }

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val url = constructUrl(args.safeUserId, text)

        return text.toSpannableStringBuilder()
            .replace(0, text.length, args.safeResources.getString(R.string.view_bbcode_attachment_link))
            .linkifyUrl(url)
    }

    override fun canOptimize(recursiveChildren: List<BBTree>): Boolean {
        val firstChild = recursiveChildren.firstOrNull()

        return if (firstChild?.prototype === TextPrototype) {
            !isImage(firstChild.args.safeText) && super.canOptimize(recursiveChildren)
        } else {
            false
        }
    }

    private fun isImage(attachment: CharSequence) =
        imageExtensions.any { attachment.endsWith(it, true) }

    private fun constructUrl(userId: String, attachment: CharSequence) = ProxerUrls.webBase.newBuilder()
        .addPathSegments("media/kunena/attachments/$userId/${attachment.replace(whitespaceRegex, "")}")
        .build()
}
