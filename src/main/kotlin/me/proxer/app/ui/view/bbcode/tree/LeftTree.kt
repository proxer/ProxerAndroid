package me.proxer.app.ui.view.bbcode.tree

import android.content.Context
import android.text.Layout.Alignment.ALIGN_NORMAL
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AlignmentSpan
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import me.proxer.app.ui.view.bbcode.applyToViews
import me.proxer.app.ui.view.bbcode.prototype.LeftPrototype

/**
 * @author Ruben Gees
 */
class LeftTree(parent: BBTree?, children: MutableList<BBTree> = mutableListOf()) : BBTree(parent, children) {

    override val prototype = LeftPrototype

    override fun makeViews(context: Context): List<View> {
        val childViews = super.makeViewsWithoutMerging(context)

        return applyToViews(childViews) { view: View ->
            if (view is TextView) {
                view.text = SpannableStringBuilder(view.text).apply {
                    setSpan(AlignmentSpan.Standard(ALIGN_NORMAL), 0, view.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            } else if (view is ImageView) {
                (view.layoutParams as? LinearLayout.LayoutParams)?.gravity = Gravity.START
            }
        }
    }
}
