package me.proxer.app.ui.view.bbcode.prototype

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.text.set
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.extension.toast

/**
 * @author Ruben Gees
 */
object MapPrototype : TextMutatorPrototype, AutoClosingPrototype {

    private const val ZOOM_ARGUMENT = "zoom"

    private val zoomAttributeRegex = Regex("zoom *= *(.+?)( |$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *map( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *map *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val zoom = BBUtils.cutAttribute(code, zoomAttributeRegex)?.toIntOrNull()

        return BBTree(this, parent, args = BBArgs(custom = arrayOf(ZOOM_ARGUMENT to zoom)))
    }

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val zoom = args[ZOOM_ARGUMENT] as Int?

        val zoomUriPart = if (zoom != null) "&z=$zoom" else ""
        val uri = Uri.parse("geo:0,0?q=$text$zoomUriPart")

        return text.toSpannableStringBuilder().apply {
            replace(0, length, args.safeResources.getString(R.string.view_bbcode_map_link))

            this[0..length] = UriClickableSpan(uri)
        }
    }

    private class UriClickableSpan(private val uri: Uri) : ClickableSpan() {

        override fun onClick(widget: View) {
            try {
                widget.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (error: ActivityNotFoundException) {
                widget.context.toast(widget.context.getString(R.string.view_bbcode_map_no_activity_error))
            }
        }
    }
}
