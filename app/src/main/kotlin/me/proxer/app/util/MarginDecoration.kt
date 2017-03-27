package me.proxer.app.util

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.view.ViewGroup
import me.proxer.app.R

/**
 * @author Ruben Gees
 */
class MarginDecoration(context: Context, private val spanCount: Int) : RecyclerView.ItemDecoration() {

    private val margin = context.resources.getDimensionPixelSize(R.dimen.item_margin)
    private val largeMargin: Int
    private val smallMargin: Int

    init {
        val maxNumberOfSpaces = spanCount - 1
        val totalSpaceToSplitBetweenItems = maxNumberOfSpaces * margin * 2

        largeMargin = totalSpaceToSplitBetweenItems / spanCount
        smallMargin = margin * 2 - largeMargin
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        val position = parent.getChildLayoutPosition(view)
        val spanIndex = spanIndex(view.layoutParams)
        val itemCount = parent.adapter.itemCount

        val isAtTop = position - spanCount < 0
        val isAtBottom = position >= itemCount - spanCount

        when (isAtTop) {
            true -> outRect.top = 0
            false -> outRect.top = margin
        }

        when (isAtBottom) {
            true -> outRect.bottom = 0
            false -> outRect.bottom = margin
        }

        if (spanCount > 1) {
            outRect.left = when (spanIndex) {
                0 -> 0
                spanCount - 1 -> largeMargin
                1, spanCount - 2 -> smallMargin
                else -> margin
            }

            outRect.right = when (spanIndex) {
                0 -> largeMargin
                spanCount - 1 -> 0
                1, spanCount - 2 -> smallMargin
                else -> margin
            }
        } else {
            outRect.left = 0
            outRect.right = 0
        }
    }

    private fun spanIndex(layoutParams: ViewGroup.LayoutParams) = when (layoutParams) {
        is StaggeredGridLayoutManager.LayoutParams -> layoutParams.spanIndex
        is GridLayoutManager.LayoutParams -> layoutParams.spanIndex
        else -> 1
    }
}
