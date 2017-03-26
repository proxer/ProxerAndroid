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
        val isAtBottom = isAtBottom(itemCount, position)

        if (!isAtTop) {
            outRect.top = margin
        } else {
            outRect.top = 0
        }

        if (!isAtBottom) {
            outRect.bottom = margin
        } else {
            outRect.bottom = 0
        }

        if (spanCount > 1) {
            if (spanIndex == 0) {
                outRect.left = 0
                outRect.right = largeMargin

                return
            }

            if (spanIndex == spanCount - 1) {
                outRect.left = largeMargin
                outRect.right = 0

                return
            }

            if (spanIndex == 1) {
                outRect.left = smallMargin
            } else {
                outRect.left = margin
            }

            if (spanIndex == spanCount - 2) {
                outRect.right = smallMargin
            } else {
                outRect.right = margin
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

    private fun isAtBottom(itemCount: Int, position: Int): Boolean {
        val result = position >= itemCount - spanCount

        return result
    }
}
