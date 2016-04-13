package com.proxerme.app.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.proxerme.app.R;
import com.proxerme.app.adapter.PagingAdapter;
import com.proxerme.app.util.listener.StartReachedRecyclerOnScrollListener;
import com.proxerme.library.event.IListEvent;
import com.proxerme.library.event.error.ErrorEvent;
import com.proxerme.library.interfaces.IdItem;

import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class PollingPagingFragment<T extends IdItem & Parcelable,
        A extends PagingAdapter<T, ?>, E extends IListEvent<T>, EE extends ErrorEvent>
        extends PagingFragment<T, A, E, EE> {

    private static final int POLLING_INTERVAL = 5000;

    @Bind(R.id.fragment_paging_notification_container)
    ViewGroup notificationContainer;
    @Bind(R.id.fragment_paging_notification)
    TextView notification;

    private int newItems;

    @Nullable
    private Handler handler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            newItems = savedInstanceState.getInt("new_items");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container, savedInstanceState);

        list.addOnScrollListener(new StartReachedRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onStartReached() {
                notificationContainer.setVisibility(View.GONE);
            }
        });

        if (newItems > 0) {
            notificationContainer.setVisibility(View.VISIBLE);
        } else {
            notificationContainer.setVisibility(View.GONE);
        }

        return result;
    }

    @Override
    public void onResume() {
        super.onStart();

        if (canLoad()) {
            startPolling();
        }
    }

    @Override
    public void onPause() {
        stopPolling();

        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("new_items", newItems);
    }

    @Override
    protected void handleResult(List<T> result, boolean insert) {
        int countBefore = adapter.getItemCount();
        String firstIdBefore = countBefore > 0 ? adapter.getItemAt(0).getId() : null;
        int[] itemPositions = new int[layoutManager.getSpanCount()];
        layoutManager.findFirstVisibleItemPositions(itemPositions);
        boolean wasAtStart = itemPositions.length > 0 && itemPositions[0] != 0;

        super.handleResult(result, insert);

        newItems = adapter.getItemCount() - countBefore;

        if (wasAtStart) {
            if (newItems > 0 && firstIdBefore != null &&
                    !firstIdBefore.equals(adapter.getItemAt(0).getId())) {
                notificationContainer.setVisibility(View.VISIBLE);

                notification.setText(getNotificationText(newItems));
            }
        }

        if (handler == null) {
            startPolling();
        }
    }

    @Override
    protected void handleError(EE errorResult) {
        super.handleError(errorResult);

        notificationContainer.setVisibility(View.GONE);

        stopPolling();
    }

    @Override
    protected View inflateLayout(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_polling_paging, container, false);
    }

    @OnClick(R.id.fragment_paging_notification_container)
    void onNotificationClick() {
        list.smoothScrollToPosition(0);

        notificationContainer.setVisibility(View.GONE);
    }

    private void startPolling() {
        handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (canLoad()) {
                    doLoad(getFirstPage(), true, false);

                    if (handler != null) {
                        handler.postDelayed(this, POLLING_INTERVAL);
                    }
                } else {
                    stopPolling();
                }
            }
        }, POLLING_INTERVAL);
    }

    private void stopPolling() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @NonNull
    protected abstract String getNotificationText(int amount);

}
