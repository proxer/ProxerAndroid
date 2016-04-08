package com.proxerme.app.fragment;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.proxerme.app.R;
import com.proxerme.app.activity.DashboardActivity;
import com.proxerme.app.activity.ImageDetailActivity;
import com.proxerme.app.activity.MessageActivity;
import com.proxerme.app.adapter.ConferenceAdapter;
import com.proxerme.app.manager.NotificationManager;
import com.proxerme.app.manager.NotificationRetrievalManager;
import com.proxerme.app.manager.StorageManager;
import com.proxerme.app.util.Utils;
import com.proxerme.app.util.helper.MaterialDrawerHelper;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.Conference;
import com.proxerme.library.event.error.ConferencesErrorEvent;
import com.proxerme.library.event.success.ConferencesEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A Fragment, showing a List of Conferences to the user.
 *
 * @author Ruben Gees
 */
public class ConferencesFragment extends LoginPollingPagingFragment<Conference, ConferenceAdapter,
        ConferencesEvent, ConferencesErrorEvent> {

    @NonNull
    public static ConferencesFragment newInstance() {
        return new ConferencesFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager.cancel(getContext(), NotificationManager.MESSAGES_NOTIFICATION);
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

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLoad(@NonNull ConferencesEvent result) {
        handleResult(result);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLoadError(@NonNull ConferencesErrorEvent errorEvent) {
        handleError(errorEvent);
    }

    @Override
    protected void handleResult(ConferencesEvent result) {
        super.handleResult(result);

        StorageManager.setNewMessages(0);
        StorageManager.resetMessagesInterval();

        if (getContext() != null) {
            NotificationRetrievalManager.retrieveMessagesLater(getContext());
        }

        if (getActivity() != null) {
            getDashboardActivity().setBadge(MaterialDrawerHelper.DRAWER_ID_MESSAGES,
                    null);
        }
    }

    @NonNull
    @Override
    protected String getNotificationText(int amount) {
        return getResources().getQuantityString(R.plurals.notification_conferences, amount, amount);
    }

    protected DashboardActivity getDashboardActivity() {
        try {
            return (DashboardActivity) getActivity();
        } catch (ClassCastException e) {
            throw new RuntimeException("Don't use this Fragment in another" +
                    " Activity than DashboardActivity.");
        }
    }
}
