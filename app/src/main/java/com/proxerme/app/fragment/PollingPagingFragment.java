package com.proxerme.app.fragment;

import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.app.adapter.PagingAdapter;
import com.proxerme.library.event.IListEvent;
import com.proxerme.library.event.error.ErrorEvent;
import com.proxerme.library.interfaces.IdItem;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class PollingPagingFragment<T extends IdItem & Parcelable,
        A extends PagingAdapter<T, ?>, E extends IListEvent<T>, EE extends ErrorEvent>
        extends PagingFragment<T, A, E, EE> {

    private static final int POLLING_INTERVAL = 5000;

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
    public void onLoad(@NonNull E result) {
        super.onLoad(result);

        if (handler == null) {
            startPolling();
        }
    }

    @Override
    public void onLoadError(@NonNull EE errorEvent) {
        super.onLoadError(errorEvent);

        stopPolling();
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
