package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.GlideRequests
import me.proxer.app.ui.view.BetterLinkGifAwareEmojiTextView
import me.proxer.app.ui.view.bbcode.BBCodeEmoticons
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBTree.Companion.ENABLE_EMOTICONS_ARGUMENT
import me.proxer.app.ui.view.bbcode.BBTree.Companion.GLIDE_ARGUMENT
import me.proxer.app.ui.view.bbcode.applyToViews

/**
 * @author Ruben Gees
 */
object RootPrototype : BBPrototype {

    override val startRegex = Regex("x^")
    override val endRegex = Regex("x^")

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val views = super.makeViews(context, children, args)

        val result = when (views.size) {
            0, 1 -> applyOnViews(views, args)
            else -> listOf(LinearLayout(context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = VERTICAL

                views.forEach { addView(it) }
            })
        }

        return applyOnViews(result, args)
    }

    fun applyOnViews(views: List<View>, args: Map<String, Any?>): List<View> {
        val enableEmotions = args[ENABLE_EMOTICONS_ARGUMENT] as Boolean?

        return when (enableEmotions) {
            true -> {
                val glide = args[GLIDE_ARGUMENT] as GlideRequests?

                when (glide) {
                    null -> views
                    else -> applyToViews(views, { view: BetterLinkGifAwareEmojiTextView ->
                        BBCodeEmoticons.replaceWithGifs(view, glide)
                    })
                }
            }
            else -> views
        }
    }
}
