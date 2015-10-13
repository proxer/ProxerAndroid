package com.proxerme.app.view;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class InitialLoadingSwipeRefreshLayout extends SwipeRefreshLayout {

    private boolean measured;
    private boolean preMeasureRefreshing;

    public InitialLoadingSwipeRefreshLayout(Context context) {
        super(context);
    }

    public InitialLoadingSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!measured) {
            measured = true;
            setRefreshing(preMeasureRefreshing);
        }
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        if (measured) {
            super.setRefreshing(refreshing);
        } else {
            preMeasureRefreshing = refreshing;
        }
    }
}
