package com.proxerme.app.fragment;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.proxerme.app.R;
import com.proxerme.app.activity.ImageDetailActivity;
import com.proxerme.app.activity.MessageActivity;
import com.proxerme.app.adapter.ConferenceAdapter;
import com.proxerme.app.util.Section;
import com.proxerme.app.util.Utils;
import com.proxerme.app.util.helper.StorageHelper;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.Conference;
import com.proxerme.library.event.error.ConferencesErrorEvent;
import com.proxerme.library.event.success.ConferencesEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 * A Fragment, showing a List of Conferences to the user.
 *
 * @author Ruben Gees
 */
public class ConferencesFragment extends LoginPollingPagingFragment<Conference, ConferenceAdapter,
        ConferencesEvent, ConferencesErrorEvent> {

    private static final int POLLING_INTERVAL = 7000;

    @NonNull
    public static ConferencesFragment newInstance() {
        return new ConferencesFragment();
    }

    @Override
    public void onResume() {
        super.onResume();

        getMainApplication().setCurrentSection(Section.CONFERENCES);
    }

    @NonNull
    @Override
    protected ConferenceAdapter createAdapter(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return new ConferenceAdapter();
        } else {
            return new ConferenceAdapter(savedInstanceState);
        }
    }

    @Override
    protected void configAdapter(@NonNull ConferenceAdapter adapter) {
        adapter.setOnConferenceInteractionListener(new ConferenceAdapter
                .OnConferenceInteractionListener() {
            @Override
            public void onConferenceClick(@NonNull View v, @NonNull Conference conference) {
                if (Utils.areActionsPossible(getActivity())) {
                    MessageActivity.navigateTo(getActivity(), conference);
                }
            }

            @Override
            public void onConferenceImageClick(@NonNull View v, @NonNull Conference conference) {
                if (!TextUtils.isEmpty(conference.getImageId())
                        && Utils.areActionsPossible(getActivity())) {
                    ImageDetailActivity.navigateTo(getActivity(), (ImageView) v,
                            UrlHolder.getUserImageUrl(conference.getImageId()));
                }
            }
        });
    }

    @Override
    protected void load(@IntRange(from = 1) int page, boolean insert) {
        ProxerConnection.loadConferences(page).execute();
    }

    @Override
    protected void cancelRequest() {
        ProxerConnection.cancel(ProxerTag.CONFERENCES);
    }

    @Override
    protected void handleResult(List<Conference> result, boolean insert) {
        super.handleResult(result, insert);

        if (insert) {
            StorageHelper.setNewMessages(0);
            StorageHelper.resetMessagesInterval();

            if (result.size() > 0) {
                StorageHelper.setLastReceivedMessageTime(result.get(0).getTime());
            }

            getMainApplication().getNotificationManager().retrieveConferencesLater(getContext());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoad(ConferencesEvent result) {
        handleResult(result);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoadError(ConferencesErrorEvent errorEvent) {
        handleError(errorEvent);
    }

    @NonNull
    @Override
    protected String getNotificationText(int amount) {
        return getResources().getQuantityString(R.plurals.notification_conferences, amount, amount);
    }

    @Override
    protected int getPollingInterval() {
        return POLLING_INTERVAL;
    }
}
