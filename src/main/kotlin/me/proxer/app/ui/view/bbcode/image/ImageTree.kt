package me.proxer.app.ui.view.bbcode.image

import android.app.Activity
import android.content.Context
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import me.proxer.app.ui.ImageDetailActivity
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.defaultLoad

/**
 * @author Ruben Gees
 */
class ImageTree(
        private val width: Int?,
        parent: BBTree?,
        children: MutableList<BBTree> = mutableListOf()
) : BBTree(parent, children) {

    override val prototype = ImagePrototype

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViewsWithoutMerging(context)
        val url = Utils.parseAndFixUrl((childViews.firstOrNull() as? TextView)?.text.toString())

        return listOf(AppCompatImageView(context).also { it: ImageView ->
            ViewCompat.setTransitionName(it, "bb_image_$url")

            it.layoutParams = LayoutParams(when (width) {
                null -> LayoutParams.MATCH_PARENT
                else -> width
            }, LayoutParams.WRAP_CONTENT)

            glide?.defaultLoad(it, url)

            if (context is Activity) {
                it.setOnClickListener { _ ->
                    if (it.drawable != null) {
                        ImageDetailActivity.navigateTo(context, url, it)
                    }
                }
            }
        })
    }
}
