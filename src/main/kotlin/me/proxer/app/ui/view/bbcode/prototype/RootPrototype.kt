package me.proxer.app.ui.view.bbcode.prototype

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.ui.view.BetterLinkGifAwareEmojiTextView
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeEmoticons
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToAllViews

/**
 * @author Ruben Gees
 */
object RootPrototype : BBPrototype {

    @Suppress("RegExpUnexpectedAnchor")
    override val startRegex = Regex("x^")

    @Suppress("RegExpUnexpectedAnchor")
    override val endRegex = Regex("x^")

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val views = super.makeViews(parent, children, args)

        val result = when (views.size) {
            0, 1 -> views
            else -> listOf(
                LinearLayout(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    orientation = VERTICAL

                    views.forEach { addView(it) }
                }
            )
        }

        return applyOnViews(result, args)
    }

    fun applyOnViews(views: List<View>, args: BBArgs) = applyToAllViews(views) { view: View ->
        if (view is BetterLinkGifAwareEmojiTextView && args.enableEmoticons) {
            val glide = args.glide

            if (glide != null) BBCodeEmoticons.replaceWithGifs(view, glide)
        }

        val layoutParams = view.layoutParams

        if (layoutParams != null) {
            val parent = view.parent

            if (parent == null || parent is FrameLayout) {
                view.layoutParams = FrameLayout.LayoutParams(layoutParams).apply {
                    if (layoutParams is LinearLayout.LayoutParams) gravity = layoutParams.gravity
                }
            }
        }
    }
}
