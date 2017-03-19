package me.proxer.app.util

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import me.proxer.app.R

class MarginDecoration(context: Context, private val columns: Int) : RecyclerView.ItemDecoration() {

    private val margin = context.resources.getDimensionPixelSize(R.dimen.item_margin)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        val position = parent.getChildLayoutPosition(view)
        val itemCount = parent.adapter.itemCount

        val isAtTop = position - columns < 0
        val isAtBottom = position >= itemCount - columns

        if (!isAtTop) {
            outRect.top = margin
        }

        if (!isAtBottom) {
            outRect.bottom = margin
        }

        if (columns > 1) {
            val isLeft = position == 0 || position % columns == 0
            val isRight = position == itemCount - 1 || (position + 1) % columns == 0

            if (!isLeft) {
                outRect.left = margin
            }

            if (!isRight) {
                outRect.right = margin
            }
        }
    }
}
