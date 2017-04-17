package me.proxer.app.util.listener

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.OnScrollListener
import android.support.v7.widget.StaggeredGridLayoutManager

/**
 * A listener for a RecyclerView, to load more items, once it reaches near the end.

 * @author Ruben Gees
 */
abstract class EndlessRecyclerOnScrollListener(private val layoutManager: RecyclerView.LayoutManager,
                                               private val visibleThreshold: Int = 5) : OnScrollListener() {

    private var lastScroll = 0L

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        if (System.currentTimeMillis() - lastScroll > 50L) {
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount
            var pastVisibleItems = 0

            when (layoutManager) {
                is StaggeredGridLayoutManager -> {
                    val firstVisibleItems = IntArray(layoutManager.spanCount).apply {
                        layoutManager.findFirstVisibleItemPositions(this)
                    }

                    if (firstVisibleItems.isNotEmpty()) {
                        pastVisibleItems = firstVisibleItems[0]
                    }
                }
                is LinearLayoutManager -> pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
            }

            if (totalItemCount > 0 && visibleItemCount + pastVisibleItems >= totalItemCount - visibleThreshold) {
                onLoadMore()
            }

            lastScroll = System.currentTimeMillis()
        }
    }

    abstract fun onLoadMore()
}
