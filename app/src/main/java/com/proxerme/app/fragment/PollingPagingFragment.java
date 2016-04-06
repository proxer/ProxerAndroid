package com.proxerme.app.fragment;

import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.TextView;

import com.proxerme.app.R;
import com.proxerme.app.adapter.PagingAdapter;
import com.proxerme.library.event.IListEvent;
import com.proxerme.library.event.error.ErrorEvent;
import com.proxerme.library.interfaces.IdItem;

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

    @Nullable
    private Handler handler;

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
    protected void handleResult(E result) {
        super.handleResult(result);

        if (handler == null) {
            startPolling();
        }
    }

    @Override
    protected void handleError(EE errorResult) {
        super.handleError(errorResult);

        stopPolling();
    }

    @OnClick(R.id.fragment_paging_notification_container)
    public void onNotificationClick() {

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

}
