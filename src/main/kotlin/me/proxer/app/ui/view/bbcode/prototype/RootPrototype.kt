package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.ui.view.BetterLinkGifAwareEmojiTextView
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeEmoticons
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews

/**
 * @author Ruben Gees
 */
object RootPrototype : BBPrototype {

    override val startRegex = Regex("x^")
    override val endRegex = Regex("x^")

    override fun makeViews(context: Context, children: List<BBTree>, args: BBArgs): List<View> {
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

    fun applyOnViews(views: List<View>, args: BBArgs) = when (args.enableEmoticons) {
        true -> {
            val glide = args.glide

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
