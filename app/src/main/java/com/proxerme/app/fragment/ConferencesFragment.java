package com.proxerme.app.fragment;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.proxerme.app.activity.ImageDetailActivity;
import com.proxerme.app.adapter.ConferenceAdapter;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.Conference;

import java.util.List;

/**
 * A Fragment, showing a List of Conferences to the user.
 *
 * @author Ruben Gees
 */
public class ConferencesFragment extends PagingFragment<Conference, ConferenceAdapter> {

    private static final int POLLING_INTERVAL = 5000;
    private Thread pollingTask;

    @NonNull
    public static ConferencesFragment newInstance() {
        return new ConferencesFragment();
    }

    @Override
    public void onStart() {
        super.onStart();

        pollingTask = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //noinspection InfiniteLoopStatement
                    while (true) {
                        Thread.sleep(POLLING_INTERVAL);

                        if (ConferencesFragment.this.getActivity() != null) {
                            ConferencesFragment.this.getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    doLoad(1, true, false);
                                }
                            });
                        }
                    }
                } catch (InterruptedException ignored) {
                }
            }
        });

        pollingTask.start();
    }

    @Override
    public void onStop() {
        super.onStop();

        pollingTask.interrupt();
        pollingTask = null;
    }

    @Override
    protected ConferenceAdapter getAdapter(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return new ConferenceAdapter();
        } else {
            return new ConferenceAdapter(savedInstanceState);
        }
    }

    @Override
    protected void load(@IntRange(from = 1) int page, boolean insert,
                        @NonNull final ProxerConnection.ResultCallback<List<Conference>> callback) {
        ProxerConnection.loadConferences(page).execute(new ProxerConnection
                .ResultCallback<List<Conference>>() {
            @Override
            public void onResult(List<Conference> conferences) {
                callback.onResult(conferences);
            }

            @Override
            public void onError(@NonNull ProxerException exception) {
                callback.onError(exception);
            }
        });
    }

    @Override
    protected void configAdapter(@NonNull ConferenceAdapter adapter) {
        adapter.setOnConferenceInteractionListener(new ConferenceAdapter
                .OnConferenceInteractionListener() {
            @Override
            public void onConferenceClick(@NonNull View v, @NonNull Conference conference) {
                //TODO
            }

            @Override
            public void onConferenceImageClick(@NonNull View v, @NonNull Conference conference) {
                if (!TextUtils.isEmpty(conference.getImageId())) {
                    ImageDetailActivity.navigateTo(getActivity(), (ImageView) v,
                            UrlHolder.getUserImage(conference.getImageId()));
                }
            }
        });
    }

    @Override
    protected void cancelRequest() {
        ProxerConnection.cancel(ProxerTag.CONFERENCES, false);
    }
}
