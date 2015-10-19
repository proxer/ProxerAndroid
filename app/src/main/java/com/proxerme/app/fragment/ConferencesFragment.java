package com.proxerme.app.fragment;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import com.proxerme.app.activity.NewsImageDetailActivity;
import com.proxerme.app.adapter.ConferenceAdapter;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.Conference;

import java.util.List;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class ConferencesFragment extends PagingFragment<Conference, ConferenceAdapter> {

    public static ConferencesFragment newInstance() {
        return new ConferencesFragment();
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
    protected void configAdapter(ConferenceAdapter adapter) {
        adapter.setOnConferenceInteractionListener(new ConferenceAdapter
                .OnConferenceInteractionListener() {
            @Override
            public void onConferenceClick(@NonNull View v, @NonNull Conference conference) {

            }

            @Override
            public void onConferenceImageClick(@NonNull View v, @NonNull Conference conference) {
                NewsImageDetailActivity.navigateTo(getActivity(), (ImageView) v,
                        UrlHolder.getUserImage(conference.getImageId()));
            }
        });
    }

    @Override
    protected void cancelRequest() {
        ProxerConnection.cancel(ProxerTag.CONFERENCES, false);
    }
}
