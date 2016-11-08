package com.proxerme.app.util.listener

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.OnScrollListener
import android.support.v7.widget.StaggeredGridLayoutManager

/**
 * A listener for a RecyclerView, to load more items, once it reaches near the end.

 * @author Ruben Gees
 */
abstract class EndlessRecyclerOnScrollListener
(private val layoutManager: RecyclerView.LayoutManager) : OnScrollListener() {

    private companion object {
        const val VISIBLE_THRESHOLD = 5
    }

    private var pastVisibleItems: Int = 0

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount

        if (layoutManager is StaggeredGridLayoutManager) {
            val firstVisibleItems = IntArray(layoutManager.spanCount)

            layoutManager.findFirstVisibleItemPositions(firstVisibleItems)

            if (firstVisibleItems.isNotEmpty()) {
                pastVisibleItems = firstVisibleItems[0]
            }
        } else if (layoutManager is LinearLayoutManager) {
            pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
        }

        if (totalItemCount > 0 && visibleItemCount + pastVisibleItems >= totalItemCount - VISIBLE_THRESHOLD) {
            onLoadMore()
        }
    }

    abstract fun onLoadMore()
}