package me.proxer.app.ui.view.bbcode.left

import android.content.Context
import android.text.Layout.Alignment.ALIGN_NORMAL
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.AlignmentSpan
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder

/**
 * @author Ruben Gees
 */
class LeftTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = LeftPrototype

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViewsWithoutMerging(context)

        return applyToViews(childViews) { view: View ->
            if (view is TextView) {
                view.text = view.text.toSpannableStringBuilder().apply {
                    setSpan(AlignmentSpan.Standard(ALIGN_NORMAL), 0, view.length(), SPAN_INCLUSIVE_EXCLUSIVE)
                }
            } else if (view is ImageView) {
                (view.layoutParams as? LinearLayout.LayoutParams)?.gravity = Gravity.START
            }
        }
    }
}
