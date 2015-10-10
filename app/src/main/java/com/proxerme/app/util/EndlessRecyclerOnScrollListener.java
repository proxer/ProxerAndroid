/*
 *   Copyright 2015 Ruben Gees
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.proxerme.app.util;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import static android.support.v7.widget.RecyclerView.OnScrollListener;

/**
 * A listener for a {@link RecyclerView}, to load more items, once it reaches near the end.
 *
 * @author Ruben Gees
 */
public abstract class EndlessRecyclerOnScrollListener extends OnScrollListener {

    private static final int VISIBLE_THRESHOLD = 5;
    private int pastVisibleItems;
    private StaggeredGridLayoutManager mLayoutManager;

    public EndlessRecyclerOnScrollListener(StaggeredGridLayoutManager mLayoutManager) {
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