package me.proxer.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView

/**
 * Stolen from here: http://stackoverflow.com/a/41547051/4279995.
 */
class PaddingDividerItemDecoration(context: Context, paddingDp: Float) : RecyclerView.ItemDecoration() {

    companion object {
        private val ATTRS = intArrayOf(android.R.attr.listDivider)
    }

    private val divider: Drawable
    private val padding: Int

    init {
        val styledAttributes = context.obtainStyledAttributes(ATTRS)

        divider = styledAttributes.getDrawable(0)

        styledAttributes.recycle()

        padding = DeviceUtils.convertDpToPx(context, paddingDp)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        val left = parent.paddingLeft + padding
        val right = parent.width - parent.paddingRight - padding

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight

            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }
}