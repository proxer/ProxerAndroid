package me.proxer.app.ui.view.bbcode.prototype

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.View
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import org.jetbrains.anko.longToast

/**
 * @author Ruben Gees
 */
object MapPrototype : TextMutatorPrototype {

    private val ZOOM_ATTRIBUTE_REGEX = Regex("zoom *= *(.+?)( |$)", REGEX_OPTIONS)
    private const val ZOOM_ARGUMENT = "zoom"

    override val startRegex = Regex(" *map( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *map *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val zoom = BBUtils.cutAttribute(code, ZOOM_ATTRIBUTE_REGEX)?.toIntOrNull()

        return BBTree(this, parent, args = mutableMapOf(ZOOM_ARGUMENT to zoom))
    }

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        val zoom = args[ZOOM_ARGUMENT] as Int?

        val zoomUriPart = if (zoom != null) "&z=$zoom" else ""
        val uri = Uri.parse("geo:0,0?q=$text$zoomUriPart")

        return globalContext.getString(R.string.view_bbcode_map_link).toSpannableStringBuilder().apply {
            setSpan(UriClickableSpan(uri), 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    private class UriClickableSpan(private val uri: Uri) : ClickableSpan() {

        override fun onClick(widget: View) {
            try {
                widget.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (error: ActivityNotFoundException) {
                widget.context.longToast(widget.context.getString(R.string.view_bbcode_map_no_activity_error))
            }
        }
    }
}
