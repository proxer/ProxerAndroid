package com.proxerme.app.util.listener;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class NotificationRecyclerOnScrollListener extends RecyclerView.OnScrollListener {

    private StaggeredGridLayoutManager mLayoutManager;

    public NotificationRecyclerOnScrollListener(@NonNull StaggeredGridLayoutManager mLayoutManager) {
        this.mLayoutManager = mLayoutManager;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        int[] firstVisibleItems = new int[mLayoutManager.getSpanCount()];

        mLayoutManager.findFirstVisibleItemPositions(firstVisibleItems);

        if (firstVisibleItems.length > 0) {
            if (firstVisibleItems[0] == 0) {
                onStartReached();
            }
        } else {
            onStartReached();
        }
    }

    public abstract void onStartReached();
}
