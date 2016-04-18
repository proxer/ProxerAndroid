package com.proxerme.app.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.proxerme.app.adapter.PagingAdapter;
import com.proxerme.library.event.IListEvent;
import com.proxerme.library.event.error.ErrorEvent;
import com.proxerme.library.interfaces.IdItem;

import java.util.List;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class PollingPagingFragment<T extends IdItem & Parcelable,
        A extends PagingAdapter<T, ?>, E extends IListEvent<T>, EE extends ErrorEvent>
        extends PagingFragment<T, A, E, EE> {

    private static final String STATE_POLLING = "polling";

    private Handler handler = new Handler();

    private boolean polling = false;
    private boolean wasPolling = false;

    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (canLoad()) {
                doLoad(getFirstPage(), true, false);

                handler.postDelayed(pollingRunnable, getPollingInterval());
            } else {
                stopPolling();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            wasPolling = savedInstanceState.getBoolean(STATE_POLLING);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (canLoad()) {
            if (wasPolling) { //The Fragment must have been active already so this is an orientation change. Restart polling immediately.
                wasPolling = false; //Reset wasPolling as it is only used for state management.

                startPolling(false);
            } else {
                startPolling(true);
            }
        }
    }

    @Override
    public void onPause() {
        if (polling) {
            wasPolling = true;
        }

        stopPolling();

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_POLLING, wasPolling);
    }

    @Override
    protected void handleResult(List<T> result, boolean insert) {
        super.handleResult(result, insert);

        startPolling(true);
    }

    @Override
    protected void handleError(EE errorResult) {
        super.handleError(errorResult);

        stopPolling();
    }

    private void startPolling(boolean delay) {
        if (!polling) {
            polling = true;

            if (delay) {
                handler.postDelayed(pollingRunnable, getPollingInterval());
            } else {
                handler.post(pollingRunnable);
            }
        }
    }

    private void stopPolling() {
        polling = false;

        handler.removeCallbacks(pollingRunnable);
    }


    protected abstract int getPollingInterval();

}
