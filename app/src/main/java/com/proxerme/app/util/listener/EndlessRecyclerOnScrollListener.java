package com.proxerme.app.util.listener;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import static android.support.v7.widget.RecyclerView.OnScrollListener;

/**
 * A listener for a RecyclerView, to load more items, once it reaches near the end.
 *
 * @author Ruben Gees
 */
public abstract class EndlessRecyclerOnScrollListener extends OnScrollListener {

    private static final int VISIBLE_THRESHOLD = 5;
    private int pastVisibleItems;
    private StaggeredGridLayoutManager mLayoutManager;

    public EndlessRecyclerOnScrollListener(@NonNull StaggeredGridLayoutManager mLayoutManager) {
        this.mLayoutManager = mLayoutManager;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        int visibleItemCount = mLayoutManager.getChildCount();
        int totalItemCount = mLayoutManager.getItemCount();
        int[] firstVisibleItems = new int[mLayoutManager.getSpanCount()];
        firstVisibleItems = mLayoutManager.findFirstVisibleItemPositions(firstVisibleItems);

        if (firstVisibleItems != null && firstVisibleItems.length > 0) {
            pastVisibleItems = firstVisibleItems[0];
        }

        if ((visibleItemCount + pastVisibleItems) >= (totalItemCount - VISIBLE_THRESHOLD)) {
            onLoadMore();
        }
    }

    public abstract void onLoadMore();
}