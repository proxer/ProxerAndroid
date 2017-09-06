package me.proxer.app.ui

import android.content.Context
import android.graphics.Canvas
import android.support.v7.widget.RecyclerView
import me.proxer.app.util.extension.getDrawableFromAttrs
import org.jetbrains.anko.childrenSequence
import org.jetbrains.anko.dip

/**
 * Stolen from here: https://stackoverflow.com/a/41547051/4279995.
 */
class PaddingDividerItemDecoration(context: Context, paddingDp: Float) : RecyclerView.ItemDecoration() {

    private val divider = context.getDrawableFromAttrs(android.R.attr.listDivider)
    private val padding = context.dip(paddingDp)

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        val left = parent.paddingLeft + padding
        val right = parent.width - parent.paddingRight - padding

        parent.childrenSequence().forEach { child ->
            val params = child.layoutParams as RecyclerView.LayoutParams

            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight

            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }
}
