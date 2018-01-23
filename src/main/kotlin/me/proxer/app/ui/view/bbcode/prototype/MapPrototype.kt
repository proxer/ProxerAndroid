package me.proxer.app.ui.view.bbcode.prototype

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import org.jetbrains.anko.longToast

/**
 * @author Ruben Gees
 */
object MapPrototype : BBPrototype {

    private val ZOOM_ATTRIBUTE_REGEX = Regex("zoom *= *(.+?)( |$)", REGEX_OPTIONS)
    private const val ZOOM_ARGUMENT = "zoom"

    override val startRegex = Regex(" *map( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *map *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val zoom = BBUtils.cutAttribute(code, ZOOM_ATTRIBUTE_REGEX)?.toIntOrNull()

        return BBTree(this, parent, args = mutableMapOf(ZOOM_ARGUMENT to zoom))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }.filterIsInstance(TextView::class.java)

        val query = childViews.firstOrNull()?.text?.toString()
        val zoom = args[ZOOM_ARGUMENT] as Int?

        val zoomUriPart = if (zoom != null) "&z=$zoom" else ""
        val uri = Uri.parse("geo:0,0?q=$query$zoomUriPart")

        return when (query) {
            null -> emptyList()
            else -> listOf(TextPrototype.makeView(context, context.getString(R.string.view_bbcode_map_link)).apply {
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View?) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (error: ActivityNotFoundException) {
                            context.longToast(context.getString(R.string.view_bbcode_map_no_activity_error))
                        }
                    }
                }

                text = text.toSpannableStringBuilder().apply {
                    setSpan(clickableSpan, 0, text.length, SPAN_INCLUSIVE_EXCLUSIVE)
                }
            })
        }
    }
}
