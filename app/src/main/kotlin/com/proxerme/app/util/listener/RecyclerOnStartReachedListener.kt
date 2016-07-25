package com.proxerme.app.util.listener

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager

/**
 * TODO: Describe class

 * @author Ruben Gees
 */
abstract class RecyclerOnStartReachedListener
(private val layoutManager: StaggeredGridLayoutManager) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        val firstVisibleItems = IntArray(layoutManager.spanCount)

        layoutManager.findFirstVisibleItemPositions(firstVisibleItems)

        if (firstVisibleItems.size > 0) {
            if (firstVisibleItems[0] == 0) {
                onStartReached()
            }
        } else {
            onStartReached()
        }
    }

    abstract fun onStartReached()
}
